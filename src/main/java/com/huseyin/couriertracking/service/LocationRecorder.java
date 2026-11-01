package com.huseyin.couriertracking.service;

import com.huseyin.couriertracking.config.StoreLoader;
import com.huseyin.couriertracking.distance.DistanceCalculator;
import com.huseyin.couriertracking.dto.LocationRequestDTO;
import com.huseyin.couriertracking.entity.Courier;
import com.huseyin.couriertracking.entity.CourierLocation;
import com.huseyin.couriertracking.entity.CourierStorePresence;
import com.huseyin.couriertracking.entity.StoreEntrance;
import com.huseyin.couriertracking.entrance.CourierEnteredStoreEvent;
import com.huseyin.couriertracking.entrance.StoreEntranceDetector;
import com.huseyin.couriertracking.exception.FutureTimeException;
import com.huseyin.couriertracking.model.Store;
import com.huseyin.couriertracking.repository.CourierLocationRepository;
import com.huseyin.couriertracking.repository.CourierRepository;
import com.huseyin.couriertracking.repository.CourierStorePresenceRepository;
import com.huseyin.couriertracking.repository.StoreEntranceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
@RequiredArgsConstructor
 class LocationRecorder {

    private static final Duration MAX_FUTURE_CLOCK = Duration.ofMinutes(5);

    private final CourierRepository courierRepository;
    private final CourierLocationRepository courierLocationRepository;
    private final CourierStorePresenceRepository presenceRepository;
    private final StoreEntranceRepository entranceRepository;

    private final DistanceCalculator distanceCalculator;
    private final Clock clock;
    private final StoreLoader storeLoader;
    private final StoreEntranceDetector entranceDetector;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void record(LocationRequestDTO request) {
        Instant receivedAt = Instant.now(clock);
        rejectIfInFuture(request.time(), receivedAt);

        Optional<Courier> existing = courierRepository.findByIdForUpdate(request.courierId());
        if (existing.isEmpty()) {
            startTracking(request, receivedAt);
            return;
        }

        Courier courier = existing.get();
        if (isDuplicate(request)) {
            log.debug("Duplicate location ignored: courier={}, time={}", request.courierId(), request.time());
            return;
        }
        if (isOutOfOrder(courier, request)) {
            saveOutOfOrder(request, receivedAt);
            return;
        }
        moveCourier(courier, request, receivedAt);
    }

    private void rejectIfInFuture(Instant time, Instant now) {
        if (time.isAfter(now.plus(MAX_FUTURE_CLOCK))) {
            throw new FutureTimeException(time, now);
        }
    }

    private void startTracking(LocationRequestDTO request, Instant receivedAt) {
        Courier courier = new Courier(request.courierId(), request.lat(), request.lng(), request.time());
        courierRepository.save(courier);
        CourierLocation courierLocation = new CourierLocation(request.courierId(), request.lat(), request.lng(), request.time(), receivedAt, false, 0.0);
        courierLocationRepository.save(courierLocation);
        updateStorePresence(courierLocation);
    }

    private boolean isDuplicate(LocationRequestDTO request) {
        return courierLocationRepository.existsByCourierIdAndRecordedAt(request.courierId(), request.time());
    }

    private boolean isOutOfOrder(Courier courier, LocationRequestDTO request) {
        return request.time().isBefore(courier.getLastRecordedAt());
    }

    private void saveOutOfOrder(LocationRequestDTO request, Instant receivedAt) {
        CourierLocation courierLocation = new CourierLocation(request.courierId(), request.lat(), request.lng(), request.time(), receivedAt, true, null);
        courierLocationRepository.save(courierLocation);
        log.warn("Out of order location stored: courier={}, time={}", request.courierId(), request.time());
    }

    private void moveCourier(Courier courier, LocationRequestDTO request, Instant receivedAt) {
        double segmentMeters = distanceCalculator.calculateDistance(courier.getLastLat(), courier.getLastLng(), request.lat(), request.lng());
        courier.moveTo(request.lat(), request.lng(), request.time(), segmentMeters);

        CourierLocation courierLocation = new CourierLocation(request.courierId(), request.lat(), request.lng(), request.time(), receivedAt, false, segmentMeters);
        courierLocationRepository.save(courierLocation);

        updateStorePresence(courierLocation);
    }

    private void updateStorePresence(CourierLocation location) {
        Map<String, CourierStorePresence> presences = presenceRepository.findByCourierId(location.getCourierId())
                .stream()
                .collect(toMap(CourierStorePresence::getStoreName, p -> p));

        for (Store store : storeLoader.findAll()) {
            double distance = distanceCalculator.calculateDistance(store.lat(), store.lng(), location.getLat(), location.getLng());
            CourierStorePresence presence = presences.get(store.name());

            switch (entranceDetector.detect(presence, distance, location.getRecordedAt())) {
                case NONE -> {
                }
                case ENTRANCE -> {
                    if (presence == null) {
                        presenceRepository.save(new CourierStorePresence(location.getCourierId(), store.name()));
                    } else {
                        presence.enter();
                    }
                    entranceRepository.save(new StoreEntrance(location.getCourierId(), store.name(), location.getId(), location.getRecordedAt()));
                    eventPublisher.publishEvent(new CourierEnteredStoreEvent(location.getCourierId(), store.name(), location.getRecordedAt()));
                }
                case REENTRY_TOO_SOON -> presence.enter();
                case EXIT -> presence.exit(location.getRecordedAt());
            }
        }
    }
}

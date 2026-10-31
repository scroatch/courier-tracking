package com.huseyin.couriertracking.service;

import com.huseyin.couriertracking.distance.DistanceCalculator;
import com.huseyin.couriertracking.dto.LocationRequest;
import com.huseyin.couriertracking.entity.Courier;
import com.huseyin.couriertracking.entity.CourierLocation;
import com.huseyin.couriertracking.exception.FutureTimeException;
import com.huseyin.couriertracking.repository.CourierLocationRepository;
import com.huseyin.couriertracking.repository.CourierRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private static final Duration MAX_FUTURE_CLOCK = Duration.ofMinutes(5);

    private final CourierRepository courierRepository;
    private final CourierLocationRepository courierLocationRepository;
    private final DistanceCalculator distanceCalculator;
    private final Clock clock;

    @Transactional
    public void record(LocationRequest request) {
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

    private void startTracking(LocationRequest request, Instant receivedAt) {
        Courier courier = new Courier(request.courierId(), request.lat(), request.lng(), request.time());
        courierRepository.save(courier);
        CourierLocation courierLocation = new CourierLocation(request.courierId(), request.lat(), request.lng(), request.time(), receivedAt, false, 0.0);
        courierLocationRepository.save(courierLocation);
    }

    private boolean isDuplicate(LocationRequest request) {
        return courierLocationRepository.existsByCourierIdAndRecordedAt(request.courierId(), request.time());
    }

    private boolean isOutOfOrder(Courier courier, LocationRequest request) {
        return request.time().isBefore(courier.getLastRecordedAt());
    }

    private void saveOutOfOrder(LocationRequest request, Instant receivedAt) {
        CourierLocation courierLocation = new CourierLocation(request.courierId(), request.lat(), request.lng(), request.time(), receivedAt, true, null);
        courierLocationRepository.save(courierLocation);
        log.warn("Out of order location stored: courier={}, time={}", request.courierId(), request.time());
    }

    private void moveCourier(Courier courier, LocationRequest request, Instant receivedAt) {
        double distanceDiff = distanceCalculator.calculateDistance(courier.getLastLat(), courier.getLastLng(), request.lat(), request.lng());

        courier.setLastLat(request.lat());
        courier.setLastLng(request.lng());
        courier.setLastRecordedAt(request.time());
        courier.setTotalDistanceMeters(distanceDiff + courier.getTotalDistanceMeters());

        CourierLocation courierLocation = new CourierLocation(request.courierId(), request.lat(), request.lng(), request.time(), receivedAt, false, distanceDiff);
        courierLocationRepository.save(courierLocation);
    }
}

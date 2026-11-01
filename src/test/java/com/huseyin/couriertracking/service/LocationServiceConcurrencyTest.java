package com.huseyin.couriertracking.service;

import com.huseyin.couriertracking.dto.LocationRequestDTO;
import com.huseyin.couriertracking.entity.Courier;
import com.huseyin.couriertracking.entity.CourierLocation;
import com.huseyin.couriertracking.repository.CourierLocationRepository;
import com.huseyin.couriertracking.repository.CourierRepository;
import com.huseyin.couriertracking.repository.CourierStorePresenceRepository;
import com.huseyin.couriertracking.repository.StoreEntranceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@ActiveProfiles("test")
class LocationServiceConcurrencyTest {

    private static final int COURIERS = 20;
    private static final double OUTSIDE_LAT = 41.0;
    private static final double OUTSIDE_LNG = 29.0;

    @Autowired
    private LocationService locationService;

    @Autowired
    private StoreEntranceRepository storeEntranceRepository;

    @Autowired
    private CourierStorePresenceRepository presenceRepository;

    @Autowired
    private CourierLocationRepository courierLocationRepository;

    @Autowired
    private CourierRepository courierRepository;

    @AfterEach
    void cleanUp() {
        storeEntranceRepository.deleteAll();
        presenceRepository.deleteAll();
        courierLocationRepository.deleteAll();
        courierRepository.deleteAll();
    }

    @Test
    void concurrentFirstLocations_bothRecorded() throws Exception {
        Instant time = Instant.now().minusSeconds(60);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < COURIERS; i++) {
                String courierId = "race-" + i;
                CountDownLatch start = new CountDownLatch(1);

                Future<?> first = executor.submit(() -> {
                    start.await();
                    locationService.record(new LocationRequestDTO(courierId, OUTSIDE_LAT, OUTSIDE_LNG, time));
                    return null;
                });
                Future<?> second = executor.submit(() -> {
                    start.await();
                    locationService.record(new LocationRequestDTO(courierId, OUTSIDE_LAT, OUTSIDE_LNG, time.plusSeconds(1)));
                    return null;
                });

                start.countDown();
                first.get(10, TimeUnit.SECONDS);
                second.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(courierRepository.count()).isEqualTo(COURIERS);
        assertThat(courierLocationRepository.count()).isEqualTo(2L * COURIERS);
    }

    @Test
    void concurrentLocations_totalMatchesSegments() throws Exception {
        String courierId = "busy";
        int threads = 10;
        Instant firstTime = Instant.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(120);
        locationService.record(new LocationRequestDTO(courierId, OUTSIDE_LAT, OUTSIDE_LNG, firstTime));

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 1; i <= threads; i++) {
                double lat = OUTSIDE_LAT + i * 0.001; // ~111 m apart
                Instant time = firstTime.plusSeconds(i);
                futures.add(executor.submit(() -> {
                    start.await();
                    locationService.record(new LocationRequestDTO(courierId, lat, OUTSIDE_LNG, time));
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        Courier courier = courierRepository.findById(courierId).orElseThrow();
        List<CourierLocation> locations = courierLocationRepository.findAll();
        double acceptedSegments = locations.stream()
                .filter(location -> !location.isOutOfOrder())
                .mapToDouble(CourierLocation::getSegmentDistanceMeters)
                .sum();

        assertThat(locations).hasSize(threads + 1);
        assertThat(courier.getTotalDistanceMeters()).isCloseTo(acceptedSegments, within(1e-6));
        assertThat(courier.getLastRecordedAt()).isEqualTo(firstTime.plusSeconds(threads));
    }
}
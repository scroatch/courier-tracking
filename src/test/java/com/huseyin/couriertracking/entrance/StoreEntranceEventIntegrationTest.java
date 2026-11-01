package com.huseyin.couriertracking.entrance;

import com.huseyin.couriertracking.dto.LocationRequestDTO;
import com.huseyin.couriertracking.repository.CourierLocationRepository;
import com.huseyin.couriertracking.repository.CourierRepository;
import com.huseyin.couriertracking.repository.CourierStorePresenceRepository;
import com.huseyin.couriertracking.repository.StoreEntranceRepository;
import com.huseyin.couriertracking.service.LocationService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class StoreEntranceEventIntegrationTest {

    private static final String ATASEHIR = "Ataşehir MMM Migros";
    private static final String COURIER_ID = "courier-it";
    private static final double ATASEHIR_LAT = 40.9923307;
    private static final double ATASEHIR_LNG = 29.1244229;

    @Autowired
    private LocationService locationService;

    @Autowired
    private MeterRegistry meterRegistry;

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
    void committedEntrance_incrementsCounter() {
        double before = currentCount();

        locationService.record(new LocationRequestDTO(COURIER_ID, ATASEHIR_LAT, ATASEHIR_LNG, Instant.now().minusSeconds(10)));

        assertThat(currentCount() - before).isEqualTo(1);
    }

    private double currentCount() {
        Counter counter = meterRegistry.find("courier.store.entrances").tag("store", ATASEHIR).counter();
        return counter == null ? 0 : counter.count();
    }

}

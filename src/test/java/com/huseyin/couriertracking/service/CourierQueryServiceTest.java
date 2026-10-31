package com.huseyin.couriertracking.service;

import com.huseyin.couriertracking.dto.EntranceResponseDTO;
import com.huseyin.couriertracking.dto.LocationRequestDTO;
import com.huseyin.couriertracking.exception.CourierNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CourierQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-31T12:00:00Z");
    private static final String COURIER_ID = "courier-1";
    private static final String ATASEHIR = "Ataşehir MMM Migros";

    private static final double ATASEHIR_LAT = 40.9923307;
    private static final double ATASEHIR_LNG = 29.1244229;
    private static final double METERS_PER_DEGREE_LAT = 111_320.0; // approximation; test points stay far from the 100 m boundary

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private CourierQueryService courierQueryService;

    @Autowired
    private LocationService locationService;

    @Test
    void entrancesNewestFirstAndLimited() {
        Instant base = NOW.minus(Duration.ofHours(1));
        locationService.record(metersNorthOfAtasehir(500, base));                  // +0:00 outside
        locationService.record(metersNorthOfAtasehir(50, base.plusSeconds(60)));   // +1:00 first entrance
        locationService.record(metersNorthOfAtasehir(200, base.plusSeconds(120))); // +2:00 exit
        locationService.record(metersNorthOfAtasehir(50, base.plusSeconds(240)));  // +4:00 back after 120s, counted

        assertThat(courierQueryService.getEntrances(COURIER_ID, 10))
                .extracting(EntranceResponseDTO::enteredAt)
                .containsExactly(base.plusSeconds(240), base.plusSeconds(60));

        assertThat(courierQueryService.getEntrances(COURIER_ID, 1)).singleElement().satisfies(entrance -> {
            assertThat(entrance.storeName()).isEqualTo(ATASEHIR);
            assertThat(entrance.enteredAt()).isEqualTo(base.plusSeconds(240));
        });
    }

    @Test
    void entrancesUnknownCourierThrowsNotFound() {
        assertThatThrownBy(() -> courierQueryService.getEntrances("unknown", 10))
                .isInstanceOf(CourierNotFoundException.class);
    }

    private LocationRequestDTO metersNorthOfAtasehir(double meters, Instant time) {
        return new LocationRequestDTO(COURIER_ID, ATASEHIR_LAT + meters / METERS_PER_DEGREE_LAT, ATASEHIR_LNG, time);
    }
}
package com.huseyin.couriertracking.service;

import com.huseyin.couriertracking.dto.LocationRequestDTO;
import com.huseyin.couriertracking.entity.Courier;
import com.huseyin.couriertracking.entity.CourierLocation;
import com.huseyin.couriertracking.entity.StoreEntrance;
import com.huseyin.couriertracking.exception.FutureTimeException;
import com.huseyin.couriertracking.repository.CourierLocationRepository;
import com.huseyin.couriertracking.repository.CourierRepository;
import com.huseyin.couriertracking.repository.CourierStorePresenceRepository;
import com.huseyin.couriertracking.repository.StoreEntranceRepository;
import jakarta.persistence.EntityManager;
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

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LocationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-31T12:00:00Z");
    private static final String COURIER_ID = "courier-1";
    private static final String ATASEHIR = "Ataşehir MMM Migros";

    private static final double ATASEHIR_LAT = 40.9923307;
    private static final double ATASEHIR_LNG = 29.1244229;
    private static final double NOVADA_LAT = 40.986106;
    private static final double NOVADA_LNG = 29.1161293;
    private static final double METERS_PER_DEGREE_LAT = 111_320.0;


    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private LocationService locationService;

    @Autowired
    private CourierStorePresenceRepository presenceRepository;

    @Autowired
    private StoreEntranceRepository storeEntranceRepository;

    @Autowired
    private CourierRepository courierRepository;

    @Autowired
    private CourierLocationRepository courierLocationRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void firstLocation_createsCourierWithZeroDistance() {
        Instant time = NOW.minusSeconds(60);

        locationService.record(request(ATASEHIR_LAT, ATASEHIR_LNG, time));
        flushAndClear();

        Courier courier = courierRepository.findById(COURIER_ID).orElseThrow();
        assertThat(courier.getTotalDistanceMeters()).isZero();
        assertThat(courier.getLastRecordedAt()).isEqualTo(time);
        assertThat(courier.getCreatedAt()).isNotNull();

        assertThat(courierLocationRepository.findAll()).singleElement().satisfies(location -> {
            assertThat(location.isOutOfOrder()).isFalse();
            assertThat(location.getSegmentDistanceMeters()).isZero();
        });
    }

    @Test
    void nextLocation_addsSegmentToTotalDistance() {
        locationService.record(request(ATASEHIR_LAT, ATASEHIR_LNG, NOW.minusSeconds(120)));
        locationService.record(request(NOVADA_LAT, NOVADA_LNG, NOW.minusSeconds(60)));
        flushAndClear();

        Courier courier = courierRepository.findById(COURIER_ID).orElseThrow();
        assertThat(courier.getTotalDistanceMeters()).isCloseTo(982.305, within(0.001));
        assertThat(courier.getLastLat()).isEqualTo(NOVADA_LAT);
        assertThat(courier.getLastLng()).isEqualTo(NOVADA_LNG);
        assertThat(courierLocationRepository.count()).isEqualTo(2);
    }

    @Test
    void duplicateLocation_isIgnored() {
        locationService.record(request(ATASEHIR_LAT, ATASEHIR_LNG, NOW.minusSeconds(120)));
        locationService.record(request(NOVADA_LAT, NOVADA_LNG, NOW.minusSeconds(60)));
        locationService.record(request(NOVADA_LAT, NOVADA_LNG, NOW.minusSeconds(60)));
        flushAndClear();

        Courier courier = courierRepository.findById(COURIER_ID).orElseThrow();
        assertThat(courier.getTotalDistanceMeters()).isCloseTo(982.305, within(0.001));
        assertThat(courierLocationRepository.count()).isEqualTo(2);
    }

    @Test
    void outOfOrderLocation_isStoredButNotCounted() {
        Instant latest = NOW.minusSeconds(60);
        Instant older = NOW.minusSeconds(120);

        locationService.record(request(ATASEHIR_LAT, ATASEHIR_LNG, latest));
        locationService.record(request(NOVADA_LAT, NOVADA_LNG, older));
        flushAndClear();

        Courier courier = courierRepository.findById(COURIER_ID).orElseThrow();
        assertThat(courier.getTotalDistanceMeters()).isZero();
        assertThat(courier.getLastRecordedAt()).isEqualTo(latest);
        assertThat(courier.getLastLat()).isEqualTo(ATASEHIR_LAT);

        CourierLocation outOfOrder = courierLocationRepository.findAll().stream()
                .filter(location -> location.getRecordedAt().equals(older))
                .findFirst()
                .orElseThrow();
        assertThat(outOfOrder.isOutOfOrder()).isTrue();
        assertThat(outOfOrder.getSegmentDistanceMeters()).isNull();
    }

    @Test
    void locationTooFarInFuture_isRejected() {
        Instant tooLate = NOW.plus(Duration.ofMinutes(5)).plusSeconds(1);

        assertThatThrownBy(() -> locationService.record(request(ATASEHIR_LAT, ATASEHIR_LNG, tooLate)))
                .isInstanceOf(FutureTimeException.class);

        assertThat(courierRepository.count()).isZero();
        assertThat(courierLocationRepository.count()).isZero();
    }

    @Test
    void locationWithinClockSkew_isAccepted() {
        Instant edge = NOW.plus(Duration.ofMinutes(5));

        locationService.record(request(ATASEHIR_LAT, ATASEHIR_LNG, edge));
        flushAndClear();

        assertThat(courierRepository.findById(COURIER_ID)).isPresent();
    }

    @Test
    void firstLocationInsideStore_recordsEntrance() {
        Instant time = NOW.minusSeconds(60);

        locationService.record(metersNorthOfAtasehir(50, time));
        flushAndClear();

        CourierLocation location = courierLocationRepository.findAll().getFirst();
        assertThat(storeEntranceRepository.findAll()).singleElement().satisfies(entrance -> {
            assertThat(entrance.getStoreName()).isEqualTo(ATASEHIR);
            assertThat(entrance.getEnteredAt()).isEqualTo(time);
            assertThat(entrance.getLocationId()).isEqualTo(location.getId());
        });
        assertThat(presenceRepository.findAll()).singleElement()
                .satisfies(presence -> assertThat(presence.isInside()).isTrue());
    }

    @Test
    void outOfOrderLocationInsideStore_doesNotRecordEntrance() {
        Instant time = NOW.minusSeconds(60);

        locationService.record(metersNorthOfAtasehir(500, time));
        locationService.record(metersNorthOfAtasehir(50, time.minusSeconds(60)));
        flushAndClear();

        assertThat(storeEntranceRepository.count()).isZero();
        assertThat(presenceRepository.count()).isZero();
        assertThat(courierLocationRepository.count()).isEqualTo(2);
    }

    @Test
    void documentedScenario_recordsTwoEntrances() {
        Instant base = NOW.minus(Duration.ofHours(1));

        locationService.record(metersNorthOfAtasehir(500, base));                  // +0:00 outside
        locationService.record(metersNorthOfAtasehir(80, base.plusSeconds(60)));   // +1:00 first entrance
        locationService.record(metersNorthOfAtasehir(50, base.plusSeconds(70)));   // +1:10 still inside
        locationService.record(metersNorthOfAtasehir(60, base.plusSeconds(300)));  // +5:00 still inside
        locationService.record(metersNorthOfAtasehir(150, base.plusSeconds(360))); // +6:00 exit
        locationService.record(metersNorthOfAtasehir(90, base.plusSeconds(400)));  // +6:40 back after 40s, not counted
        locationService.record(metersNorthOfAtasehir(200, base.plusSeconds(480))); // +8:00 exit
        locationService.record(metersNorthOfAtasehir(70, base.plusSeconds(570)));  // +9:30 back after 90s, counted
        flushAndClear();

        assertThat(storeEntranceRepository.findAll())
                .extracting(StoreEntrance::getEnteredAt)
                .containsExactlyInAnyOrder(base.plusSeconds(60), base.plusSeconds(570));

        assertThat(courierLocationRepository.count()).isEqualTo(8);

        assertThat(presenceRepository.findAll()).singleElement()
                .satisfies(presence -> assertThat(presence.isInside()).isTrue());
    }

    private LocationRequestDTO metersNorthOfAtasehir(double meters, Instant time) {
        return request(ATASEHIR_LAT + meters / METERS_PER_DEGREE_LAT, ATASEHIR_LNG, time);
    }

    private LocationRequestDTO request(double lat, double lng, Instant time) {
        return new LocationRequestDTO(COURIER_ID, lat, lng, time);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
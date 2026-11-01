package com.huseyin.couriertracking.distance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HaversineDistanceCalculatorTest {

    private static final double ONE_MILLIMETER = 0.001;
    private static final double R = HaversineDistanceCalculator.EARTH_RADIUS;
    private static final double ONE_DEGREE_ARC = R * Math.PI / 180;

    private static final double ATASEHIR_LAT = 40.9923307;
    private static final double ATASEHIR_LNG = 29.1244229;

    private final DistanceCalculator calculator = new HaversineDistanceCalculator();


    @Test
    void samePoint_returnsExactlyZero() {
        double distance = calculator.calculateDistance(ATASEHIR_LAT, ATASEHIR_LNG, ATASEHIR_LAT, ATASEHIR_LNG);

        assertThat(distance).isZero();
    }


    @Test
    void oneDegreeAlongMeridian_equalsOneDegreeArc() {
        double distance = calculator.calculateDistance(40, 29, 41, 29);

        assertThat(distance).isCloseTo(ONE_DEGREE_ARC, within(ONE_MILLIMETER));
    }


    @ParameterizedTest(name = "({0}, {1}) -> ({2}, {3})")
    @CsvSource({
            "0, 0, 0, 180",
            "41, 29, -41, -151"
    })
    void antipodalPoints_returnHalfCircumference(double lat1, double lng1, double lat2, double lng2) {
        double distance = calculator.calculateDistance(lat1, lng1, lat2, lng2);

        assertThat(distance)
                .isNotNaN()
                .isCloseTo(Math.PI * R, within(ONE_MILLIMETER));
    }


    @Test
    void atasehirToNovada_matchesReferenceValue() {
        double distance = calculator.calculateDistance(
                ATASEHIR_LAT, ATASEHIR_LNG,
                40.986106, 29.1161293);
        assertThat(distance).isCloseTo(981.658, within(ONE_MILLIMETER));
    }

    @Test
    void pointHundredMetersNorth_isHundredMetersAway() {
        double dLat = 100.0 / ONE_DEGREE_ARC;   // ≈ 0.00089932°

        double distance = calculator.calculateDistance(
                ATASEHIR_LAT, ATASEHIR_LNG,
                ATASEHIR_LAT + dLat, ATASEHIR_LNG);

        assertThat(distance).isCloseTo(100.0, within(ONE_MILLIMETER));
    }

    @Test
    void pointHundredMetersEast_isHundredMetersAway() {
        double dLng = 100.0 / (ONE_DEGREE_ARC * Math.cos(Math.toRadians(ATASEHIR_LAT)));
        double distance = calculator.calculateDistance(
                ATASEHIR_LAT, ATASEHIR_LNG,
                ATASEHIR_LAT, ATASEHIR_LNG + dLng);
        assertThat(distance).isCloseTo(100.0, within(ONE_MILLIMETER));
    }

    @ParameterizedTest(name = "({0}, {1}) <-> ({2}, {3})")
    @CsvSource({
            "40.9923307, 29.1244229, 40.986106,  29.1161293",
            "41.0066851, 28.6552262, 41.055783,  29.0210292",
            "-33.8688,   151.2093,   51.5074,    -0.1278"
    })
    void distance_isSymmetric(double lat1, double lng1, double lat2, double lng2) {
        double forward = calculator.calculateDistance(lat1, lng1, lat2, lng2);
        double backward = calculator.calculateDistance(lat2, lng2, lat1, lng1);

        assertThat(forward).isCloseTo(backward, within(ONE_MILLIMETER));
    }

}
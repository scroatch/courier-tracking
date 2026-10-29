package com.huseyin.couriertracking.distance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;


class DistanceModelConsistencyTest {

    private static final double MAX_RELATIVE_DIFFERENCE = 0.005;

    private final DistanceCalculator haversine = new HaversineDistanceCalculator();
    private final DistanceCalculator karney = new KarneyDistanceCalculator();

    @ParameterizedTest(name = "({0}, {1}) -> ({2}, {3})")
    @CsvSource({
            "40.9923307, 29.1244229, 40.986106,  29.1161293",   // Ataşehir - Novada
            "41.0066851, 28.6552262, 41.055783,  29.0210292",   // Beylikdüzü - Ortaköy
            "41.055783,  29.0210292, 40.9632463, 29.0630908"    // Ortaköy - Caddebostan
    })
    @DisplayName("Model arasındaki fark %0.5'in altındadır")
    void sphericalAndEllipsoidalModels_agreeWithinHalfPercent(double lat1, double lng1, double lat2, double lng2) {
        double spherical = haversine.calculateDistance(lat1, lng1, lat2, lng2);
        double ellipsoidal = karney.calculateDistance(lat1, lng1, lat2, lng2);
        double relativeDifference = Math.abs(spherical - ellipsoidal) / ellipsoidal;

        assertThat(relativeDifference).isLessThan(MAX_RELATIVE_DIFFERENCE);
    }
}
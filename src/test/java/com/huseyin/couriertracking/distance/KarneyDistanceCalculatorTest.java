package com.huseyin.couriertracking.distance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class KarneyDistanceCalculatorTest {

    private static final double ONE_MILLIMETER = 0.001;
    private static final double ATASEHIR_LAT = 40.9923307;
    private static final double ATASEHIR_LNG = 29.1244229;

    private final DistanceCalculator calculator = new KarneyDistanceCalculator();

    @Test
    @DisplayName("Aynı nokta için mesafe tam olarak sıfırdır")
    void samePoint_returnsExactlyZero() {
        double distance = calculator.calculateDistance(ATASEHIR_LAT, ATASEHIR_LNG, ATASEHIR_LAT, ATASEHIR_LNG);

        assertThat(distance).isZero();
    }

    @Test
    @DisplayName("Ataşehir - Novada mağazaları arası referans mesafe")
    void atasehirToNovada_matchesReferenceValue() {
        double distance = calculator.calculateDistance(ATASEHIR_LAT, ATASEHIR_LNG, 40.986106, 29.1161293);

        assertThat(distance).isCloseTo(982.305, within(ONE_MILLIMETER));
    }

    @Test
    @DisplayName("enlem önce, boylam sonra verilir")
    void argumentOrder_latitudeBeforeLongitude() {

        double distance = calculator.calculateDistance(40, 29, 41, 29);

        assertThat(distance).isCloseTo(111_044.261, within(ONE_MILLIMETER));
    }


    @ParameterizedTest(name = "({0}, {1}) <-> ({2}, {3}) = {4} m")
    @CsvSource({
            "40.9923307, 29.1244229,  40.986106, 29.1161293,       982.305",
            "41.0066851, 28.6552262,  41.055783, 29.0210292,     31241.832",
            "-33.8688,   151.2093,    51.5074,   -0.1278,     16989295.771"
    })
    @DisplayName("Mesafe simetriktir")
    void distance_isSymmetric(double lat1, double lng1, double lat2, double lng2, double expected) {
        double forward = calculator.calculateDistance(lat1, lng1, lat2, lng2);
        double backward = calculator.calculateDistance(lat2, lng2, lat1, lng1);

        assertThat(forward).isCloseTo(expected, within(ONE_MILLIMETER));
        assertThat(backward).isCloseTo(expected, within(ONE_MILLIMETER));
    }
}
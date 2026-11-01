package com.huseyin.couriertracking.distance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DistanceCalculatorConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(DistanceCalculatorConfig.class);

    @Test
    void noStrategy_defaultsToKarney() {
        runner.run(context -> assertThat(context).getBean(DistanceCalculator.class)
                .isInstanceOf(KarneyDistanceCalculator.class));
    }

    @Test
    void haversineStrategy_createsHaversine() {
        runner.withPropertyValues("courier.distance.strategy=HAVERSINE")
                .run(context -> assertThat(context).getBean(DistanceCalculator.class)
                        .isInstanceOf(HaversineDistanceCalculator.class));
    }

    @Test
    void unknownStrategy_failsStartup() {
        runner.withPropertyValues("courier.distance.strategy=VINCENTY")
                .run(context -> assertThat(context).hasFailed());
    }
}
package com.huseyin.couriertracking.distance;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DistanceProperties.class)
class DistanceCalculatorConfig {

    @Bean
    DistanceCalculator distanceCalculator(DistanceProperties properties) {
        DistanceStrategy strategy = properties.strategy();
        log.info("Distance calculation strategy: {}", strategy);
        return switch (strategy) {
            case KARNEY_WGS84 -> new KarneyDistanceCalculator();
            case HAVERSINE -> new HaversineDistanceCalculator();
        };
    }
}

package com.huseyin.couriertracking.distance;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "courier.distance")
public record DistanceProperties(@DefaultValue("KARNEY_WGS84") DistanceStrategy strategy) {
}

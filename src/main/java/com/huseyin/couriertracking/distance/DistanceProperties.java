package com.huseyin.couriertracking.distance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "courier.distance")
public record DistanceProperties(@DefaultValue("KARNEY_WGS84") @NotNull DistanceStrategy strategy,
                                 @DefaultValue("30m") @NotNull Duration maxGap,
                                 @DefaultValue("25") @PositiveOrZero double minMovementMeters) {
}

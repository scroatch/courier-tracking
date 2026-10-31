package com.huseyin.couriertracking.entrance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "courier.entrance")
public record EntranceProperties(@DefaultValue("100") @Positive double radiusMeters,
                                 @DefaultValue("60s") @NotNull Duration reentryInterval) {
}
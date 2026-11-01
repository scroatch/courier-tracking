package com.huseyin.couriertracking.dto;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record LocationRequestDTO(@NotBlank @Size(max = 64) String courierId,
                                 @NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
                                 @NotNull @DecimalMin("-180") @DecimalMax("180") Double lng,
                                 @NotNull Instant time) {

    public LocationRequestDTO {
        if (time != null) {
            time = time.truncatedTo(ChronoUnit.MICROS); // DB stores microseconds
        }
    }
}

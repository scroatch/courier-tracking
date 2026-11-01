package com.huseyin.couriertracking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record LocationRequestDTO(@Schema(example = "courier-1") @NotBlank @Size(max = 64) String courierId,
                                 @Schema(example = "40.9923307") @NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
                                 @Schema(example = "29.1244229") @NotNull @DecimalMin("-180") @DecimalMax("180") Double lng,
                                 @NotNull Instant time) {

    public LocationRequestDTO {
        if (time != null) {
            time = time.truncatedTo(ChronoUnit.MICROS); // DB stores microseconds
        }
    }
}

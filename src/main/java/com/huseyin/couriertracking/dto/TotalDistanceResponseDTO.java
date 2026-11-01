package com.huseyin.couriertracking.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TotalDistanceResponseDTO(@Schema(example = "courier-1") String courierId, @Schema(example = "982.305") double totalDistanceMeters) {
}

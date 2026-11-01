package com.huseyin.couriertracking.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record EntranceResponseDTO(@Schema(example = "Ataşehir MMM Migros") String storeName, Instant enteredAt) {
}
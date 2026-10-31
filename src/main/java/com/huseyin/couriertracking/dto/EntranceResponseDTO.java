package com.huseyin.couriertracking.dto;

import java.time.Instant;

public record EntranceResponseDTO(String storeName, Instant enteredAt) {
}
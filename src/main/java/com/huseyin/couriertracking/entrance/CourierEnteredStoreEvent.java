package com.huseyin.couriertracking.entrance;

import java.time.Instant;

public record CourierEnteredStoreEvent(String courierId, String storeName, Instant enteredAt) {
}

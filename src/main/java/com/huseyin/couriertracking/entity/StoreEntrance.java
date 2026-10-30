package com.huseyin.couriertracking.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreEntrance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String courierId;
    private String storeName;
    private Long locationId;
    private Instant enteredAt;

    public StoreEntrance(String courierId, String storeName, Long locationId, Instant enteredAt) {
        this.courierId = courierId;
        this.storeName = storeName;
        this.locationId = locationId;
        this.enteredAt = enteredAt;
    }
}

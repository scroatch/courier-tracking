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
public class CourierLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String courierId;
    private double lat;
    private double lng;
    private Instant recordedAt;
    private Instant receivedAt;
    private boolean outOfOrder;
    private Double segmentDistanceMeters;

    public CourierLocation(String courierId, double lat, double lng, Instant recordedAt, Instant receivedAt,
                           boolean outOfOrder, Double segmentDistanceMeters) {
        this.courierId = courierId;
        this.lat = lat;
        this.lng = lng;
        this.recordedAt = recordedAt;
        this.receivedAt = receivedAt;
        this.outOfOrder = outOfOrder;
        this.segmentDistanceMeters = segmentDistanceMeters;
    }
}

package com.huseyin.couriertracking.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Courier implements Persistable<String> {

    @Id
    private String id;

    // reference point for distance: GPS jitter around it is not counted
    private double lastLat;
    private double lastLng;
    private Instant lastConfirmedAt; // last time a location confirmed the courier was near the reference point
    private Instant lastRecordedAt;
    private double totalDistanceMeters;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Transient
    private boolean isNew = true;

    public Courier(String id, double lastLat, double lastLng, Instant lastRecordedAt) {
        this.id = id;
        this.lastLat = lastLat;
        this.lastLng = lastLng;
        this.lastConfirmedAt = lastRecordedAt;
        this.lastRecordedAt = lastRecordedAt;
        this.totalDistanceMeters = 0;
    }

    public void moveTo(double lat, double lng, Instant recordedAt, double segmentMeters) {
        this.lastLat = lat;
        this.lastLng = lng;
        this.lastConfirmedAt = recordedAt;
        this.lastRecordedAt = recordedAt;
        this.totalDistanceMeters += segmentMeters;
    }

    public void confirmPosition(Instant recordedAt) {
        this.lastConfirmedAt = recordedAt;
        this.lastRecordedAt = recordedAt;
    }

    public void skipLocation(Instant recordedAt) {
        this.lastRecordedAt = recordedAt;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}
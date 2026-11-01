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

    // last counted position: GPS jitter around it is not counted as distance
    private double lastLat;
    private double lastLng;
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
        this.lastRecordedAt = lastRecordedAt;
        this.totalDistanceMeters = 0;
    }

    public void moveTo(double lat, double lng, Instant recordedAt, double segmentMeters) {
        this.lastLat = lat;
        this.lastLng = lng;
        this.lastRecordedAt = recordedAt;
        this.totalDistanceMeters += segmentMeters;
    }

    public void stayUntil(Instant recordedAt) {
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
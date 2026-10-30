package com.huseyin.couriertracking.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Courier implements Persistable<String> {

    @Id
    @Setter(AccessLevel.NONE)
    private String id;

    private double lastLat;
    private double lastLng;
    private Instant lastRecordedAt;
    private double totalDistanceMeters;

    @CreationTimestamp
    @Column(updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @UpdateTimestamp
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

    @Transient
    @Setter(AccessLevel.NONE)
    private boolean isNew = true;

    public Courier(String id, double lastLat, double lastLng, Instant lastRecordedAt) {
        this.id = id;
        this.lastLat = lastLat;
        this.lastLng = lastLng;
        this.lastRecordedAt = lastRecordedAt;
        this.totalDistanceMeters = 0;
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
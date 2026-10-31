package com.huseyin.couriertracking.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourierStorePresence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String courierId;
    private String storeName;
    private boolean inside;
    private Instant lastExitAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public CourierStorePresence(String courierId, String storeName) {
        this.courierId = courierId;
        this.storeName = storeName;
        this.inside = true;
    }

    public void enter() {
        this.inside = true;
    }

    public void exit(Instant at) {
        this.inside = false;
        this.lastExitAt = at;
    }
}

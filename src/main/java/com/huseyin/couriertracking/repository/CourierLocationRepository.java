package com.huseyin.couriertracking.repository;

import com.huseyin.couriertracking.entity.CourierLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface CourierLocationRepository extends JpaRepository<CourierLocation, Long> {

    boolean existsByCourierIdAndRecordedAt(String courierId, Instant recordedAt);
}

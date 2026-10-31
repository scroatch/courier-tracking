package com.huseyin.couriertracking.repository;

import com.huseyin.couriertracking.entity.CourierStorePresence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourierStorePresenceRepository extends JpaRepository<CourierStorePresence, Long> {
    List<CourierStorePresence> findByCourierId(String courierId);
}

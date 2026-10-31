package com.huseyin.couriertracking.repository;

import com.huseyin.couriertracking.entity.StoreEntrance;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreEntranceRepository extends JpaRepository<StoreEntrance, Long> {
    List<StoreEntrance> findByCourierIdOrderByEnteredAtDesc(String courierId, Limit limit);
}

package com.huseyin.couriertracking.service;

import com.huseyin.couriertracking.entity.Courier;
import com.huseyin.couriertracking.exception.CourierNotFoundException;
import com.huseyin.couriertracking.repository.CourierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourierQueryService {

    private final CourierRepository courierRepository;

    @Transactional(readOnly = true)
    public double getTotalTravelDistance(String courierId) {
        return courierRepository.findById(courierId)
                .map(Courier::getTotalDistanceMeters)
                .orElseThrow(() -> new CourierNotFoundException(courierId));
    }
}

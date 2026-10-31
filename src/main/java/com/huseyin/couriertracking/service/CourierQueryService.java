package com.huseyin.couriertracking.service;

import com.huseyin.couriertracking.dto.EntranceResponseDTO;
import com.huseyin.couriertracking.entity.Courier;
import com.huseyin.couriertracking.exception.CourierNotFoundException;
import com.huseyin.couriertracking.repository.CourierRepository;
import com.huseyin.couriertracking.repository.StoreEntranceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourierQueryService {

    private final CourierRepository courierRepository;
    private final StoreEntranceRepository storeEntranceRepository;

    @Transactional(readOnly = true)
    public double getTotalTravelDistance(String courierId) {
        return courierRepository.findById(courierId)
                .map(Courier::getTotalDistanceMeters)
                .orElseThrow(() -> new CourierNotFoundException(courierId));
    }

    @Transactional(readOnly = true)
    public List<EntranceResponseDTO> getEntrances(String courierId, int limit) {
        if (!courierRepository.existsById(courierId)) {
            throw new CourierNotFoundException(courierId);
        }
        return storeEntranceRepository.findByCourierIdOrderByEnteredAtDesc(courierId, Limit.of(limit)).stream()
                .map(entrance -> new EntranceResponseDTO(entrance.getStoreName(), entrance.getEnteredAt()))
                .toList();
    }
}

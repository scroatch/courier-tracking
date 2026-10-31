package com.huseyin.couriertracking.controller;

import com.huseyin.couriertracking.dto.TotalDistanceResponseDTO;
import com.huseyin.couriertracking.service.CourierQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/couriers")
@RequiredArgsConstructor
public class CourierController {

    private final CourierQueryService courierQueryService;

    @GetMapping("/{courierId}/total-distance")
    public TotalDistanceResponseDTO getTotalDistance(@PathVariable String courierId) {
        return new TotalDistanceResponseDTO(courierId, courierQueryService.getTotalTravelDistance(courierId));
    }
}

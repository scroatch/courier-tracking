package com.huseyin.couriertracking.controller;

import com.huseyin.couriertracking.dto.EntranceResponseDTO;
import com.huseyin.couriertracking.dto.TotalDistanceResponseDTO;
import com.huseyin.couriertracking.service.CourierQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/couriers")
@RequiredArgsConstructor
public class CourierController {

    private final CourierQueryService courierQueryService;

    @GetMapping("/{courierId}/total-distance")
    public TotalDistanceResponseDTO getTotalDistance(@PathVariable String courierId) {
        return new TotalDistanceResponseDTO(courierId, courierQueryService.getTotalTravelDistance(courierId));
    }

    @GetMapping("/{courierId}/entrances")
    public List<EntranceResponseDTO> getEntrances(@PathVariable String courierId, @RequestParam(defaultValue = "50") @Min(1) @Max(500) int limit) {
        return courierQueryService.getEntrances(courierId, limit);
    }
}

package com.huseyin.couriertracking.controller;

import com.huseyin.couriertracking.dto.EntranceResponseDTO;
import com.huseyin.couriertracking.dto.TotalDistanceResponseDTO;
import com.huseyin.couriertracking.service.CourierQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/couriers")
@RequiredArgsConstructor
@Tag(name = "Couriers")
public class CourierController {

    private final CourierQueryService courierQueryService;

    @Operation(summary = "Total distance travelled by a courier, in meters")
    @ApiResponse(responseCode = "200", description = "Total distance")
    @ApiResponse(responseCode = "404", description = "Courier not found", content = @Content)
    @GetMapping("/{courierId}/total-distance")
    public TotalDistanceResponseDTO getTotalDistance(@PathVariable String courierId) {
        return new TotalDistanceResponseDTO(courierId, courierQueryService.getTotalTravelDistance(courierId));
    }

    @Operation(summary = "Store entrances of a courier, newest first")
    @ApiResponse(responseCode = "200", description = "Entrances, newest first")
    @ApiResponse(responseCode = "400", description = "Limit outside 1-500", content = @Content)
    @ApiResponse(responseCode = "404", description = "Courier not found", content = @Content)
    @GetMapping("/{courierId}/entrances")
    public List<EntranceResponseDTO> getEntrances(@PathVariable String courierId, @RequestParam(defaultValue = "50") @Min(1) @Max(500) int limit) {
        return courierQueryService.getEntrances(courierId, limit);
    }
}

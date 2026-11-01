package com.huseyin.couriertracking.controller;

import com.huseyin.couriertracking.dto.LocationRequestDTO;
import com.huseyin.couriertracking.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "Locations")
public class LocationController {

    private final LocationService locationService;

    @Operation(summary = "Record a courier location")
    @ApiResponse(responseCode = "204", description = "Accepted; duplicate and out-of-order locations are also accepted")
    @ApiResponse(responseCode = "400", description = "Invalid location or time too far in the future", content = @Content)
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void record(@Valid @RequestBody LocationRequestDTO request) {
        locationService.record(request);
    }
}

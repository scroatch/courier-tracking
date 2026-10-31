package com.huseyin.couriertracking.controller;

import com.huseyin.couriertracking.dto.LocationRequestDTO;
import com.huseyin.couriertracking.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void record(@Valid @RequestBody LocationRequestDTO request) {
        locationService.record(request);
    }
}

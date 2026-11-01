package com.huseyin.couriertracking.service;

import com.huseyin.couriertracking.dto.LocationRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRecorder locationRecorder;

    public void record(LocationRequestDTO request) {
        try {
            locationRecorder.record(request);
        } catch (DataIntegrityViolationException e) {
            log.info("Concurrent write for courier {}, retrying once", request.courierId());
            locationRecorder.record(request);
        }
    }
}
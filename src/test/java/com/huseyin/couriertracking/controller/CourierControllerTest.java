package com.huseyin.couriertracking.controller;

import com.huseyin.couriertracking.dto.EntranceResponseDTO;
import com.huseyin.couriertracking.exception.CourierNotFoundException;
import com.huseyin.couriertracking.service.CourierQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourierController.class)
class CourierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourierQueryService courierQueryService;

    @Test
    void totalDistanceForExistingCourier_returnsMeters() throws Exception {
        given(courierQueryService.getTotalTravelDistance("courier-1")).willReturn(982.305);

        mockMvc.perform(get("/api/v1/couriers/courier-1/total-distance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courierId").value("courier-1"))
                .andExpect(jsonPath("$.totalDistanceMeters").value(982.305));
    }

    @Test
    void totalDistanceForUnknownCourier_returnsNotFound() throws Exception {
        given(courierQueryService.getTotalTravelDistance("unknown"))
                .willThrow(new CourierNotFoundException("unknown"));

        mockMvc.perform(get("/api/v1/couriers/unknown/total-distance"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void entrances_returnsList() throws Exception{
        given(courierQueryService.getEntrances("courier-1" , 50))
                .willReturn(List.of(
                        new EntranceResponseDTO("Ataşehir MMM Migros", Instant.parse("2026-10-31T11:09:30Z")),
                        new EntranceResponseDTO("Novada MMM Migros", Instant.parse("2026-10-31T11:01:00Z"))));

        mockMvc.perform(get("/api/v1/couriers/courier-1/entrances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].storeName").value("Ataşehir MMM Migros"))
                .andExpect(jsonPath("$[0].enteredAt").value("2026-10-31T11:09:30Z"))
                .andExpect(jsonPath("$[1].storeName").value("Novada MMM Migros"));
    }

    @Test
    void entrancesWithoutLimit_usesDefault() throws Exception {
        given(courierQueryService.getEntrances("courier-1", 50)).willReturn(List.of());

        mockMvc.perform(get("/api/v1/couriers/courier-1/entrances"))
                .andExpect(status().isOk());

        verify(courierQueryService).getEntrances("courier-1", 50);
    }

    @Test
    void entrancesLimitBelowMinimum_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/couriers/courier-1/entrances").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(courierQueryService);
    }

    @Test
    void entrancesLimitAboveMaximum_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/couriers/courier-1/entrances").param("limit", "501"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(courierQueryService);
    }

    @Test
    void entrancesForUnknownCourier_returnsNotFound() throws Exception {
        given(courierQueryService.getEntrances("unknown", 50))
                .willThrow(new CourierNotFoundException("unknown"));

        mockMvc.perform(get("/api/v1/couriers/unknown/entrances"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void unexpectedError_returnsProblemDetailWithoutInternals() throws Exception {
        given(courierQueryService.getTotalTravelDistance("courier-1"))
                .willThrow(new IllegalStateException("connection pool exhausted"));

        mockMvc.perform(get("/api/v1/couriers/courier-1/total-distance"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Unexpected error"));
    }
}
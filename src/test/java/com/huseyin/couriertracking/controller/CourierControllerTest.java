package com.huseyin.couriertracking.controller;

import com.huseyin.couriertracking.exception.CourierNotFoundException;
import com.huseyin.couriertracking.service.CourierQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
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
    void existingCourierTotalDistance() throws Exception {
        given(courierQueryService.getTotalTravelDistance("courier-1")).willReturn(982.305);

        mockMvc.perform(get("/api/v1/couriers/courier-1/total-distance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courierId").value("courier-1"))
                .andExpect(jsonPath("$.totalDistanceMeters").value(982.305));
    }

    @Test
    void unknownCourier() throws Exception {
        given(courierQueryService.getTotalTravelDistance("unknown"))
                .willThrow(new CourierNotFoundException("unknown"));

        mockMvc.perform(get("/api/v1/couriers/unknown/total-distance"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
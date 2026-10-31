package com.huseyin.couriertracking.controller;

import com.huseyin.couriertracking.dto.LocationRequestDTO;
import com.huseyin.couriertracking.exception.FutureTimeException;
import com.huseyin.couriertracking.service.LocationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationController.class)
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocationService locationService;

    @Test
    void validRequest() throws Exception {
        send("""
                {"courierId": "courier-1", "lat": 40.9923307, "lng": 29.1244229, "time": "2026-10-31T12:00:00Z"}
                """)
                .andExpect(status().isNoContent());

        verify(locationService).record(any(LocationRequestDTO.class));
    }

    @Test
    void missingLat() throws Exception {
        send("""
                {"courierId": "courier-1", "lng": 29.1244229, "time": "2026-10-31T12:00:00Z"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lat").exists());

        verifyNoInteractions(locationService);
    }

    @Test
    void latOutOfRange() throws Exception {
        send("""
                {"courierId": "courier-1", "lat": 95, "lng": 29.1244229, "time": "2026-10-31T12:00:00Z"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lat").exists());

        verifyNoInteractions(locationService);
    }

    @Test
    void blankCourierId() throws Exception {
        send("""
                {"courierId": "", "lat": 40.9923307, "lng": 29.1244229, "time": "2026-10-31T12:00:00Z"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.courierId").exists());

        verifyNoInteractions(locationService);
    }

    @Test
    void malformedJson() throws Exception {
        send("{\"courierId\": \"courier-1\", \"lat\": ")
                .andExpect(status().isBadRequest());

        verifyNoInteractions(locationService);
    }

    @Test
    void futureTime() throws Exception {
        doThrow(new FutureTimeException(Instant.parse("2026-10-31T13:00:00Z"), Instant.parse("2026-10-31T12:00:00Z")))
                .when(locationService).record(any());

        send("""
                {"courierId": "courier-1", "lat": 40.9923307, "lng": 29.1244229, "time": "2026-10-31T13:00:00Z"}
                """)
                .andExpect(status().isBadRequest());
    }

    private ResultActions send(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/locations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
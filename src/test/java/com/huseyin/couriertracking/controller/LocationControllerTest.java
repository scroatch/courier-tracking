package com.huseyin.couriertracking.controller;

import com.huseyin.couriertracking.dto.LocationRequestDTO;
import com.huseyin.couriertracking.exception.FutureTimeException;
import com.huseyin.couriertracking.service.LocationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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
    void validRequest_returnsNoContent() throws Exception {
        send("""
                {"courierId": "courier-1", "lat": 40.9923307, "lng": 29.1244229, "time": "2026-10-31T12:00:00Z"}
                """)
                .andExpect(status().isNoContent());

        verify(locationService).record(any(LocationRequestDTO.class));
    }

    @Test
    void missingLat_returnsBadRequest() throws Exception {
        send("""
                {"courierId": "courier-1", "lng": 29.1244229, "time": "2026-10-31T12:00:00Z"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lat").exists());

        verifyNoInteractions(locationService);
    }

    @Test
    void latOutOfRange_returnsBadRequest() throws Exception {
        send("""
                {"courierId": "courier-1", "lat": 95, "lng": 29.1244229, "time": "2026-10-31T12:00:00Z"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lat").exists());

        verifyNoInteractions(locationService);
    }

    @Test
    void blankCourierId_returnsBadRequest() throws Exception {
        send("""
                {"courierId": "", "lat": 40.9923307, "lng": 29.1244229, "time": "2026-10-31T12:00:00Z"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.courierId").exists());

        verifyNoInteractions(locationService);
    }

    @Test
    void malformedJson_returnsBadRequest() throws Exception {
        send("{\"courierId\": \"courier-1\", \"lat\": ")
                .andExpect(status().isBadRequest());

        verifyNoInteractions(locationService);
    }

    @Test
    void futureTime_returnsBadRequest() throws Exception {
        doThrow(new FutureTimeException(Instant.parse("2026-10-31T13:00:00Z"), Instant.parse("2026-10-31T12:00:00Z")))
                .when(locationService).record(any());

        send("""
                {"courierId": "courier-1", "lat": 40.9923307, "lng": 29.1244229, "time": "2026-10-31T13:00:00Z"}
                """)
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            missing time      | {"courierId": "courier-1", "lat": 40.99, "lng": 29.12}                               | time
            missing courierId | {"lat": 40.99, "lng": 29.12, "time": "2026-10-31T12:00:00Z"}                        | courierId
            lng out of range  | {"courierId": "courier-1", "lat": 40.99, "lng": 181, "time": "2026-10-31T12:00:00Z"} | lng
            """)
    void invalidField_returnsBadRequest(String name, String body, String field) throws Exception {
        send(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors." + field).exists());

        verifyNoInteractions(locationService);
    }

    @Test
    void courierIdTooLong_returnsBadRequest() throws Exception {
        send("""
                {"courierId": "%s", "lat": 40.9923307, "lng": 29.1244229, "time": "2026-10-31T12:00:00Z"}
                """.formatted("c".repeat(65)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.courierId").exists());

        verifyNoInteractions(locationService);
    }

    private ResultActions send(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/locations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
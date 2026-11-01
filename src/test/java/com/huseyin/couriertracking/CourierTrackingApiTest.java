package com.huseyin.couriertracking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CourierTrackingApiTest {

    private static final String COURIER_ID = "e2e-courier";
    private static final double ATASEHIR_LAT = 40.9923307;
    private static final double ATASEHIR_LNG = 29.1244229;
    private static final double METERS_PER_DEGREE_LAT = 111_320.0; // approximation; test points stay far from the 100 m boundary

    @Autowired
    private MockMvc mockMvc;

    @Test
    void locationStream_producesEntrancesAndDistance() throws Exception {
        Instant base = Instant.now().truncatedTo(ChronoUnit.SECONDS).minus(Duration.ofHours(1));

        postLocation(500, base);                  // +0:00 outside
        postLocation(50, base.plusSeconds(60));   // +1:00 first entrance
        postLocation(200, base.plusSeconds(120)); // +2:00 exit
        postLocation(50, base.plusSeconds(240));  // +4:00 back after 120s, counted
        postLocation(50, base.plusSeconds(240));  // client retry, ignored

        mockMvc.perform(get("/api/v1/couriers/{courierId}/entrances", COURIER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].storeName").value("Ataşehir MMM Migros"))
                .andExpect(jsonPath("$[0].enteredAt").value(base.plusSeconds(240).toString()))
                .andExpect(jsonPath("$[1].enteredAt").value(base.plusSeconds(60).toString()));

        mockMvc.perform(get("/api/v1/couriers/{courierId}/total-distance", COURIER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDistanceMeters", closeTo(750.0, 5.0)));
    }

    @Test
    void unknownCourier_returnsProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/couriers/{courierId}/total-distance", "nobody"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("nobody not found"));
    }

    @Test
    void apiDocs_listAllEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/locations']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/couriers/{courierId}/total-distance']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/couriers/{courierId}/entrances']").exists());
    }

    private void postLocation(double metersNorth, Instant time) throws Exception {
        String body = String.format(Locale.ROOT, """
                {"courierId": "%s", "lat": %.7f, "lng": %.7f, "time": "%s"}
                """, COURIER_ID, ATASEHIR_LAT + metersNorth / METERS_PER_DEGREE_LAT, ATASEHIR_LNG, time);

        mockMvc.perform(post("/api/v1/locations").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
    }
}
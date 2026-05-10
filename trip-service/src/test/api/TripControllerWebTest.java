package org.example.taxi.trip.api;

import org.example.taxi.trip.domain.TripStatus;
import org.example.taxi.trip.security.JwtAuthFilter;
import org.example.taxi.trip.service.TripManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripController.class)
@AutoConfigureMockMvc(addFilters = false)
class TripControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TripManagementService service;
    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void getTripReturnsTripData() throws Exception {
        TripDtos.TripResponse response = new TripDtos.TripResponse(
                1L, 1L, 2L, TripStatus.DRIVER_ASSIGNED, "A", "B", 10.0, BigDecimal.valueOf(500.00), null);
        when(service.getTrip(1L)).thenReturn(response);

        mockMvc.perform(get("/trips/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("DRIVER_ASSIGNED"))
                .andExpect(jsonPath("$.price").value(500.0));
    }
}

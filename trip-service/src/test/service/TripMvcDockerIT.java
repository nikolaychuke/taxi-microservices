package service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import config.RabbitConfig;
import domain.Trip;
import domain.TripStatus;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import repository.TripRepository;
import trip.TripServiceApplication;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(classes = TripServiceApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",
        "trip.pricing.tariff-per-km=50",
        "app.jwt.secret=the-secret-key-is-32-characters-123"
})
@AutoConfigureMockMvc
@Import(TripMvcDockerIT.RabbitMockConfig.class)
class TripMvcDockerIT {

    @Configuration
    static class RabbitMockConfig {
        @Bean
        @Primary
        RabbitTemplate rabbitTemplate() {
            RabbitTemplate tpl = mock(RabbitTemplate.class);
            when(tpl.convertSendAndReceive(anyString(), anyString(), any(Object.class)))
                    .thenAnswer(invocation -> {
                        String queue = invocation.getArgument(1);
                        if (RabbitConfig.Q_PASSENGER_EXISTS.equals(queue)) {
                            return Map.of("exists", true);
                        }
                        if (RabbitConfig.Q_DRIVER_ASSIGN.equals(queue)) {
                            return Map.of("driverId", 50L);
                        }
                        return null;
                    });
            doNothing().when(tpl).convertAndSend(anyString(), anyString(), any(Object.class));
            return tpl;
        }
    }

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    TripRepository tripRepository;

    @Test
    void jwtRequiredForProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/trips/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/trips/stats/daily")).andExpect(status().isUnauthorized());
    }

    @Test
    void createTripComputesPriceDistanceTimesTariff() throws Exception {
        String token = token();

        mockMvc.perform(post("/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passenger_id\":1,\"origin\":\"O\",\"destination\":\"D\",\"distance_km\":12.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceKm").value(12.5))
                .andExpect(jsonPath("$.price").value(625))
                .andExpect(jsonPath("$.driverId").value(500))
                .andExpect(jsonPath("$.status").value("DRIVER_ASSIGNED"));
    }

    @Test
    void dailyStatsReturnsCountAndAverage() throws Exception {
        String token = token();
        Trip a = new Trip();
        a.setPassengerId(1L);
        a.setDriverId(1L);
        a.setStatus(TripStatus.COMPLETED);
        a.setOrigin("a");
        a.setDestination("b");
        a.setDistanceKm(2.0);
        a.setPrice(new java.math.BigDecimal("100.00"));
        tripRepository.save(a);
        Trip b = new Trip();
        b.setPassengerId(1L);
        b.setDriverId(1L);
        b.setStatus(TripStatus.COMPLETED);
        b.setOrigin("c");
        b.setDestination("d");
        b.setDistanceKm(4.0);
        b.setPrice(new java.math.BigDecimal("200.00"));
        tripRepository.save(b);

        mockMvc.perform(get("/trips/stats/daily")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripsCount").value(2))
                .andExpect(jsonPath("$.averagePrice").value(150.0));
    }

    @Test
    void ratingAcceptedOnlyAfterCompleted() throws Exception {
        String token = token();
        MvcResult created = mockMvc.perform(post("/trips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passenger_id\":3,\"origin\":\"P\",\"destination\":\"Q\",\"distance_km\":1}"))
                .andExpect(status().isOk())
                .andReturn();
        long tripId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/trips/" + tripId + "/rating")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5}"))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertTrue(
                        result.getResponse().getStatus() >= 400));

        mockMvc.perform(patch("/trips/" + tripId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/trips/" + tripId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"STARTED\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/trips/" + tripId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/trips/" + tripId + "/rating")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5));
    }

    private String token() throws Exception {
        MvcResult auth = mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode n = objectMapper.readTree(auth.getResponse().getContentAsString());
        return n.get("token").asText();
    }
}

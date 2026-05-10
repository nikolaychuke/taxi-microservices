package api;

import domain.TripStatus;
import security.JwtAuthFilter;
import service.TripManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TripController.class)
@Import({TripController.class, TripControllerWebTest.WebTestContext.class})
@AutoConfigureMockMvc(addFilters = false)
class TripControllerWebTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class,
            RabbitAutoConfiguration.class
    })
    static class WebTestContext {
    }

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

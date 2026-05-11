package api;

import domain.DriverStatus;
import security.JwtAuthFilter;
import service.UserManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import({UserController.class, UserControllerWebTest.WebTestContext.class})
@AutoConfigureMockMvc(addFilters = false)
class UserControllerWebTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class,
            RabbitAutoConfiguration.class,
            RedisAutoConfiguration.class
    })
    static class WebTestContext {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserManagementService service;
    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void getDriverReturnsDriverProfile() throws Exception {
        UserDtos.DriverResponse response = new UserDtos.DriverResponse(
                1L, "Egor", "test@mail.com", "+70000000000", "LIC-302", DriverStatus.AVAILABLE);
        when(service.getDriver(1L)).thenReturn(response);

        mockMvc.perform(get("/drivers/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }
}

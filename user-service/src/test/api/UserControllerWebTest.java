package org.example.taxi.user.api;

import org.example.taxi.user.domain.DriverStatus;
import org.example.taxi.user.security.JwtAuthFilter;
import org.example.taxi.user.service.UserManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserManagementService service;
    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void getDriverReturnsDriverProfile() throws Exception {
        UserDtos.DriverResponse response = new UserDtos.DriverResponse(
                1L, "Petr", "petr@mail.com", "+70000000002", "LIC-001", DriverStatus.AVAILABLE);
        when(service.getDriver(1L)).thenReturn(response);

        mockMvc.perform(get("/drivers/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }
}

package org.example.taxi.notification.api;

import org.example.taxi.notification.domain.NotificationTask;
import org.example.taxi.notification.domain.NotificationTaskStatus;
import org.example.taxi.notification.domain.RecipientType;
import org.example.taxi.notification.security.JwtAuthFilter;
import org.example.taxi.notification.service.NotificationTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationTaskService service;
    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void getNotificationsByTripReturnsTaskList() throws Exception {
        NotificationTask task = new NotificationTask();
        task.setTripId(1L);
        task.setRecipientType(RecipientType.PASSENGER);
        task.setRecipientId(1L);
        task.setMessage("Trip updated");
        task.setStatus(NotificationTaskStatus.SENT);
        when(service.getByTripId(1L)).thenReturn(List.of(task));

        mockMvc.perform(get("/notifications").param("trip_id", "1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tripId").value(1))
                .andExpect(jsonPath("$[0].status").value("SENT"));
    }
}

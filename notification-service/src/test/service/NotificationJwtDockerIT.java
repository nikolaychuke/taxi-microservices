package service;

import domain.NotificationTask;
import domain.NotificationTaskStatus;
import domain.RecipientType;
import com.fasterxml.jackson.databind.ObjectMapper;
import messaging.NotificationQueueListener;
import notification.NotificationServiceApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import repository.NotificationTaskRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(classes = NotificationServiceApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",
        "app.jwt.secret=the-secret-key-is-32-characters-123"
})
@AutoConfigureMockMvc
@MockBean(NotificationQueueListener.class)
class NotificationJwtDockerIT {

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
    NotificationTaskRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAllInBatch();
    }

    @Test
    void notificationsListRequiresJwtButWorksWithBearer() throws Exception {
        mockMvc.perform(get("/notifications").param("trip_id", "1")).andExpect(status().isUnauthorized());

        String tokenJson = mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn().getResponse().getContentAsString();
        String token = new ObjectMapper().readTree(tokenJson).get("token").asText();

        NotificationTask task = new NotificationTask();
        task.setTripId(1L);
        task.setRecipientType(RecipientType.PASSENGER);
        task.setRecipientId(2L);
        task.setStatus(NotificationTaskStatus.SENT);
        repository.save(task);

        mockMvc.perform(get("/notifications").param("trip_id", "1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tripId").value(1))
                .andExpect(jsonPath("$[0].recipientType").value("PASSENGER"));
    }
}

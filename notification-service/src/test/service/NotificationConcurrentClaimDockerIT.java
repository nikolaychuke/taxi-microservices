package service;

import domain.NotificationTask;
import domain.NotificationTaskStatus;
import domain.RecipientType;
import messaging.NotificationQueueListener;
import notification.NotificationServiceApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import repository.NotificationTaskRepository;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(classes = NotificationServiceApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",
        "app.jwt.secret=the-secret-key-is-32-characters-123"
})
@MockBean(NotificationQueueListener.class)
class NotificationConcurrentClaimDockerIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    NotificationTaskService taskService;
    @Autowired
    NotificationTaskRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAllInBatch();
    }

    @Test
    void parallelWorkersClaimDistinctPendingTasks() throws Exception {
        int tasks = 6;
        for (int i = 0; i < tasks; i++) {
            NotificationTask t = new NotificationTask();
            t.setTripId((long) (i + 1));
            t.setRecipientType(RecipientType.PASSENGER);
            t.setRecipientId(1L);
            t.setStatus(NotificationTaskStatus.PENDING);
            t.setAttempts(0);
            repository.save(t);
        }

        CountDownLatch ready = new CountDownLatch(tasks);
        CountDownLatch go = new CountDownLatch(1);
        Set<Long> claimedIds = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(tasks);

        for (int i = 0; i < tasks; i++) {
            pool.submit(() -> {
                try {
                    ready.countDown();
                    go.await(60, TimeUnit.SECONDS);
                    NotificationTask locked = taskService.lockNextPendingTask();
                    assertTrue(locked != null, "EROR");
                    claimedIds.add(locked.getId());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            });
        }

        assertTrue(ready.await(60, TimeUnit.SECONDS));
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(90, TimeUnit.SECONDS));

        assertEquals(tasks, claimedIds.size(),
                "EROR");

        assertEquals(tasks,
                repository.findAll().stream()
                        .filter(t -> t.getStatus() == NotificationTaskStatus.PROCESSING).count(),
                "EROR");
    }
}

package service;

import domain.Driver;
import domain.DriverStatus;
import messaging.UserRpcListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import repository.DriverRepository;
import user.UserServiceApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(classes = UserServiceApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",
        "app.jwt.secret=the-secret-key-is-32-characters-123"
})
@MockBean(UserRpcListener.class)
class DriverAssignConcurrencyDockerIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.data.redis.host", redis::getHost);
        r.add("spring.data.redis.port", () -> String.valueOf(redis.getMappedPort(6379)));
    }

    @Autowired
    UserManagementService userManagementService;
    @Autowired
    DriverRepository driverRepository;

    @BeforeEach
    void clean() {
        driverRepository.deleteAllInBatch();
    }

    @Test
    void concurrentAssignNeverGivesSameDriverTwice() throws Exception {
        int n = 4;
        for (int i = 0; i < n; i++) {
            Driver d = new Driver();
            d.setName("D-" + i);
            d.setEmail("d-" + i + "@concurrency.test");
            d.setPhone("+790000000" + String.format("%02d", i));
            d.setLicenseNumber("LIC-" + i);
            d.setStatus(DriverStatus.AVAILABLE);
            driverRepository.save(d);
        }
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch go = new CountDownLatch(1);
        List<Long> assignedIds = Collections.synchronizedList(new ArrayList<>());
        ExecutorService pool = Executors.newFixedThreadPool(n);
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                try {
                    ready.countDown();
                    go.await(60, TimeUnit.SECONDS);
                    Long id = userManagementService.assignFreeDriverAtomically();
                    assertTrue(id != null, "ERROR");
                    assignedIds.add(id);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            });
        }
        assertTrue(ready.await(60, TimeUnit.SECONDS));
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS));
        assertEquals(n, assignedIds.size());
        assertEquals(n, new HashSet<>(assignedIds).size(),
                "ERROR");
        long busy = driverRepository.findAll().stream().filter(d -> d.getStatus() == DriverStatus.BUSY).count();
        assertEquals(n, busy);
    }

    @Test
    void assignUpdatesDriverToBusy() {
        Driver d = new Driver();
        d.setName("Solo");
        d.setEmail("solo@concurrency.test");
        d.setPhone("+79001112233");
        d.setLicenseNumber("LIC-SOLO");
        d.setStatus(DriverStatus.AVAILABLE);
        Driver saved = driverRepository.save(d);
        driverRepository.flush();

        Long id = userManagementService.assignFreeDriverAtomically();

        assertEquals(saved.getId(), id);
        Driver loaded = driverRepository.findById(id).orElseThrow();
        assertEquals(DriverStatus.BUSY, loaded.getStatus());
    }
}

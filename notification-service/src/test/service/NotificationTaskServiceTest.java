package org.example.taxi.notification.service;

import org.example.taxi.notification.domain.NotificationTask;
import org.example.taxi.notification.domain.NotificationTaskStatus;
import org.example.taxi.notification.repository.NotificationTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationTaskServiceTest {

    @Mock
    private NotificationTaskRepository repository;

    private NotificationTaskService service;

    @BeforeEach
    void setUp() {
        service = new NotificationTaskService(repository);
    }

    @Test
    void lockNextPendingTaskMarksTaskAsProcessing() {
        NotificationTask task = new NotificationTask();
        task.setStatus(NotificationTaskStatus.PENDING);
        when(repository.findNextPendingTaskForUpdate()).thenReturn(Optional.of(task));

        NotificationTask locked = service.lockNextPendingTask();

        assertEquals(NotificationTaskStatus.PROCESSING, locked.getStatus());
    }

    @Test
    void markFailedAndRetrySetsFailedAfterThreeAttempts() {
        NotificationTask task = new NotificationTask();
        task.setAttempts(2);
        task.setStatus(NotificationTaskStatus.PENDING);
        when(repository.findById(1L)).thenReturn(Optional.of(task));

        service.markFailedAndRetry(1L);

        assertEquals(3, task.getAttempts());
        assertEquals(NotificationTaskStatus.FAILED, task.getStatus());
    }
}

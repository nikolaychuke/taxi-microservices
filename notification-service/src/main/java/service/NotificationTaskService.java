package org.example.taxi.notification.service;

import org.example.taxi.notification.domain.NotificationTask;
import org.example.taxi.notification.domain.NotificationTaskStatus;
import org.example.taxi.notification.domain.RecipientType;
import org.example.taxi.notification.messaging.NotificationMessage;
import org.example.taxi.notification.repository.NotificationTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationTaskService {
    private final NotificationTaskRepository repository;

    public NotificationTaskService(NotificationTaskRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void createTasks(NotificationMessage message) {
        NotificationTask passengerTask = new NotificationTask();
        passengerTask.setTripId(message.tripId());
        passengerTask.setRecipientType(RecipientType.PASSENGER);
        passengerTask.setRecipientId(message.passengerId());
        passengerTask.setMessage(message.message());
        repository.save(passengerTask);

        if (message.driverId() != null) {
            NotificationTask driverTask = new NotificationTask();
            driverTask.setTripId(message.tripId());
            driverTask.setRecipientType(RecipientType.DRIVER);
            driverTask.setRecipientId(message.driverId());
            driverTask.setMessage(message.message());
            repository.save(driverTask);
        }
    }

    public List<NotificationTask> getByTripId(Long tripId) {
        return repository.findByTripIdOrderByCreatedAtAsc(tripId);
    }

    @Transactional
    public NotificationTask lockNextPendingTask() {
        NotificationTask task = repository.findNextPendingTaskForUpdate().orElse(null);
        if (task != null) {
            task.setStatus(NotificationTaskStatus.PROCESSING);
        }
        return task;
    }

    @Transactional
    public void markSent(Long taskId) {
        NotificationTask task = repository.findById(taskId).orElseThrow();
        task.setStatus(NotificationTaskStatus.SENT);
    }

    @Transactional
    public void markFailedAndRetry(Long taskId) {
        NotificationTask task = repository.findById(taskId).orElseThrow();
        int attempts = task.getAttempts() + 1;
        task.setAttempts(attempts);
        task.setStatus(attempts >= 3 ? NotificationTaskStatus.FAILED : NotificationTaskStatus.PENDING);
    }
}

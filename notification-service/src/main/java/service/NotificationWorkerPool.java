package service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import domain.NotificationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class NotificationWorkerPool {
    private static final Logger log = LoggerFactory.getLogger(NotificationWorkerPool.class);

    private final NotificationTaskService taskService;
    private final int workersCount;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private ExecutorService executor;

    public NotificationWorkerPool(NotificationTaskService taskService,
                                  @Value("${notification.worker.threads:4}") int workersCount) {
        this.taskService = taskService;
        this.workersCount = workersCount;
    }

    @PostConstruct
    public void start() {
        executor = Executors.newFixedThreadPool(workersCount);
        for (int i = 0; i < workersCount; i++) {
            executor.submit(this::workerLoop);
        }
    }

    private void workerLoop() {
        while (running.get()) {
            try {
                NotificationTask task = taskService.lockNextPendingTask();
                if (task == null) {
                    Thread.sleep(500);
                    continue;
                }
                processTask(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("Worker error", e);
            }
        }
    }

    private void processTask(NotificationTask task) throws InterruptedException {
        try {
            log.info("Sending notification task={} recipientType={} recipientId={} message={}",
                    task.getId(), task.getRecipientType(), task.getRecipientId(), task.getMessage());
            Thread.sleep(1000);
            taskService.markSent(task.getId());
        } catch (Exception ex) {
            taskService.markFailedAndRetry(task.getId());
        }
    }

    @PreDestroy
    public void shutdown() {
        running.set(false);
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}

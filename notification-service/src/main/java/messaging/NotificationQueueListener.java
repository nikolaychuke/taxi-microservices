package messaging;

import config.RabbitConfig;
import service.NotificationTaskService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationQueueListener {
    private final NotificationTaskService taskService;

    public NotificationQueueListener(NotificationTaskService taskService) {
        this.taskService = taskService;
    }

    @RabbitListener(queues = RabbitConfig.Q_NOTIFICATION_CREATE)
    public void onNotificationRequested(NotificationMessage message) {
        taskService.createTasks(message);
    }
}

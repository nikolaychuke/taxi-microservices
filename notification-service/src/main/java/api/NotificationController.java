package api;

import domain.NotificationTask;
import messaging.NotificationMessage;
import service.NotificationTaskService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class NotificationController {
    private final NotificationTaskService taskService;

    public NotificationController(NotificationTaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/notifications")
    public void addTask(@RequestBody NotificationMessage message) {
        taskService.createTasks(message);
    }

    @GetMapping("/notifications")
    public List<NotificationTask> getByTrip(@RequestParam("trip_id") Long tripId) {
        return taskService.getByTripId(tripId);
    }
}

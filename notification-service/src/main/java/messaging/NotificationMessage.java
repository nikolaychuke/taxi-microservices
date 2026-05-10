package messaging;

public record NotificationMessage(Long tripId, Long passengerId, Long driverId, String status, String message) {
}

package org.example.taxi.user.messaging;

import org.example.taxi.user.config.RabbitConfig;
import org.example.taxi.user.domain.DriverStatus;
import org.example.taxi.user.service.UserManagementService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class UserRpcListener {
    private final UserManagementService service;

    public UserRpcListener(UserManagementService service) {
        this.service = service;
    }

    @RabbitListener(queues = RabbitConfig.Q_PASSENGER_EXISTS)
    public Map<String, Object> passengerExists(Map<String, Object> request) {
        Long passengerId = ((Number) request.get("passengerId")).longValue();
        Map<String, Object> response = new HashMap<>();
        response.put("exists", service.passengerExists(passengerId));
        return response;
    }

    @RabbitListener(queues = RabbitConfig.Q_DRIVER_ASSIGN)
    public Map<String, Object> assignDriver(Map<String, Object> ignored) {
        Long driverId = service.assignFreeDriverAtomically();
        Map<String, Object> response = new HashMap<>();
        response.put("driverId", driverId);
        return response;
    }

    @RabbitListener(queues = RabbitConfig.Q_DRIVER_STATUS_UPDATE)
    public void updateDriverStatus(Map<String, Object> request) {
        Long driverId = ((Number) request.get("driverId")).longValue();
        DriverStatus status = DriverStatus.valueOf(String.valueOf(request.get("status")));
        service.setDriverStatus(driverId, status);
    }
}
package org.example.taxi.user.api;

import org.example.taxi.user.domain.DriverStatus;

public class UserDtos {
    public record PassengerCreateRequest(String name, String email, String phone) {}
    public record PassengerResponse(Long id, String name, String email, String phone) {}
    public record DriverCreateRequest(String name, String email, String phone, String licenseNumber) {}
    public record DriverResponse(Long id, String name, String email, String phone, String licenseNumber, DriverStatus status) {}
    public record DriverStatusUpdateRequest(DriverStatus status) {}
}

package service;

import api.TripDtos;
import config.RabbitConfig;
import domain.Trip;
import domain.TripStatus;
import messaging.NotificationMessage;
import repository.TripRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.List;
import java.util.Map;

@Service
public class TripManagementService {
    private final TripRepository tripRepository;
    private final RabbitTemplate rabbitTemplate;
    private final BigDecimal tariffPerKm;

    public TripManagementService(TripRepository tripRepository,
                                 RabbitTemplate rabbitTemplate,
                                 @Value("${trip.pricing.tariff-per-km:50}") BigDecimal tariffPerKm) {
        this.tripRepository = tripRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.tariffPerKm = tariffPerKm;
    }

    @Transactional
    public TripDtos.TripResponse createTrip(TripDtos.CreateTripRequest request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> passengerResult =
                (Map<String, Object>) rabbitTemplate.convertSendAndReceive(
                        "", RabbitConfig.Q_PASSENGER_EXISTS, Map.of("passengerId", request.passengerId()));
        if (passengerResult == null || !Boolean.TRUE.equals(passengerResult.get("exists"))) {
            throw new IllegalArgumentException("Passenger not found");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> driverResult =
                (Map<String, Object>) rabbitTemplate.convertSendAndReceive(
                        "", RabbitConfig.Q_DRIVER_ASSIGN, Map.of("request", "assign"));
        Long assignedDriverId = driverResult == null || driverResult.get("driverId") == null
                ? null
                : ((Number) driverResult.get("driverId")).longValue();

        Trip trip = new Trip();
        trip.setPassengerId(request.passengerId());
        trip.setDriverId(assignedDriverId);
        trip.setOrigin(request.origin());
        trip.setDestination(request.destination());
        if (request.distanceKm() == null) {
            throw new IllegalArgumentException("distance_km is required for price calculation (distance × tariff)");
        }
        double distance = request.distanceKm();
        if (distance <= 0) {
            throw new IllegalArgumentException("Distance must be greater than zero");
        }
        trip.setDistanceKm(distance);
        trip.setPrice(BigDecimal.valueOf(distance).multiply(tariffPerKm).setScale(2, RoundingMode.HALF_UP));
        trip.setStatus(assignedDriverId != null ? TripStatus.DRIVER_ASSIGNED : TripStatus.CREATED);
        Trip saved = tripRepository.save(trip);
        sendStatusNotification(saved);
        return toResponse(saved);
    }

    public TripDtos.TripResponse getTrip(Long id) {
        return toResponse(tripRepository.findById(id).orElseThrow());
    }

    public List<TripDtos.TripResponse> getTripsByPassenger(Long passengerId) {
        return tripRepository.findByPassengerIdOrderByCreatedAtDesc(passengerId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public TripDtos.TripResponse updateStatus(Long id, TripStatus status) {
        Trip trip = tripRepository.findById(id).orElseThrow();
        validateStatusTransition(trip.getStatus(), status);
        trip.setStatus(status);
        if (trip.getDriverId() != null) {
            if (status == TripStatus.ACCEPTED) {
                rabbitTemplate.convertAndSend("", RabbitConfig.Q_DRIVER_STATUS_UPDATE,
                        Map.of("driverId", trip.getDriverId(), "status", "BUSY"));
            } else if (status == TripStatus.COMPLETED || status == TripStatus.CANCELLED) {
                rabbitTemplate.convertAndSend("", RabbitConfig.Q_DRIVER_STATUS_UPDATE,
                        Map.of("driverId", trip.getDriverId(), "status", "AVAILABLE"));
            }
        }
        sendStatusNotification(trip);
        return toResponse(tripRepository.save(trip));
    }

    @Transactional
    public TripDtos.TripResponse rateTrip(Long id, Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating should be from 1 to 5");
        }
        Trip trip = tripRepository.findById(id).orElseThrow();
        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new IllegalArgumentException("Trip can be rated only after completion");
        }
        trip.setRating(rating);
        return toResponse(tripRepository.save(trip));
    }

    public TripDtos.DailyStatsResponse getDailyStats(LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        Instant start = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = targetDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        long count = tripRepository.countByCreatedAtBetween(start, end);
        BigDecimal average = tripRepository.avgPriceBetween(start, end).setScale(2, RoundingMode.HALF_UP);
        return new TripDtos.DailyStatsResponse(count, average);
    }

    private void sendStatusNotification(Trip trip) {
        rabbitTemplate.convertAndSend("", RabbitConfig.Q_NOTIFICATION_CREATE,
                new NotificationMessage(
                        trip.getId(),
                        trip.getPassengerId(),
                        trip.getDriverId(),
                        trip.getStatus().name(),
                        "Trip " + trip.getId() + " changed status to " + trip.getStatus()));
    }

    private TripDtos.TripResponse toResponse(Trip t) {
        return new TripDtos.TripResponse(t.getId(), t.getPassengerId(), t.getDriverId(), t.getStatus(),
                t.getOrigin(), t.getDestination(), t.getDistanceKm(), t.getPrice(), t.getRating());
    }

    private void validateStatusTransition(TripStatus currentStatus, TripStatus nextStatus) {
        if (currentStatus == nextStatus) {
            return;
        }
        boolean valid = switch (currentStatus) {
            case CREATED -> nextStatus == TripStatus.DRIVER_ASSIGNED || nextStatus == TripStatus.CANCELLED;
            case DRIVER_ASSIGNED -> nextStatus == TripStatus.ACCEPTED || nextStatus == TripStatus.CANCELLED;
            case ACCEPTED -> nextStatus == TripStatus.STARTED || nextStatus == TripStatus.CANCELLED;
            case STARTED -> nextStatus == TripStatus.COMPLETED || nextStatus == TripStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException("Invalid status transition from " + currentStatus + " to " + nextStatus);
        }
    }
}

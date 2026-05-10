package org.example.taxi.trip.service;

import org.example.taxi.trip.api.TripDtos;
import org.example.taxi.trip.config.RabbitConfig;
import org.example.taxi.trip.domain.Trip;
import org.example.taxi.trip.domain.TripStatus;
import org.example.taxi.trip.messaging.NotificationMessage;
import org.example.taxi.trip.repository.TripRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripManagementServiceTest {

    @Mock
    private TripRepository tripRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;

    private TripManagementService service;

    @BeforeEach
    void setUp() {
        service = new TripManagementService(tripRepository, rabbitTemplate, BigDecimal.valueOf(50));
    }

    @Test
    void createTripCalculatesPriceAndAssignsDriver() {
        when(rabbitTemplate.convertSendAndReceive(eq(""), eq(RabbitConfig.Q_PASSENGER_EXISTS), any(Map.class)))
                .thenReturn(Map.of("exists", true));
        when(rabbitTemplate.convertSendAndReceive(eq(""), eq(RabbitConfig.Q_DRIVER_ASSIGN), any(Map.class)))
                .thenReturn(Map.of("driverId", 7L));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripDtos.CreateTripRequest request = new TripDtos.CreateTripRequest(1L, "A", "B", 10.0);
        TripDtos.TripResponse response = service.createTrip(request);

        assertEquals(7L, response.driverId());
        assertEquals(TripStatus.DRIVER_ASSIGNED, response.status());
        assertEquals(BigDecimal.valueOf(500.00).setScale(2), response.price());
        verify(rabbitTemplate).convertAndSend(eq(""), eq(RabbitConfig.Q_NOTIFICATION_CREATE), any(NotificationMessage.class));
    }

    @Test
    void updateStatusRejectsInvalidTransition() {
        Trip trip = new Trip();
        trip.setStatus(TripStatus.COMPLETED);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateStatus(1L, TripStatus.STARTED));
        assertTrue(ex.getMessage().contains("Invalid status transition"));
    }
}

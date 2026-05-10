package service;

import api.TripDtos;
import config.RabbitConfig;
import domain.Trip;
import domain.TripStatus;
import messaging.NotificationMessage;
import repository.TripRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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

    @Test
    void rateTripStarsMustBeBetweenOneAndFive() {
        Trip trip = new Trip();
        trip.setStatus(TripStatus.COMPLETED);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IllegalArgumentException low = assertThrows(IllegalArgumentException.class,
                () -> service.rateTrip(1L, 0));
        assertTrue(low.getMessage().contains("1 to 5"));
        IllegalArgumentException high = assertThrows(IllegalArgumentException.class,
                () -> service.rateTrip(1L, 6));
        assertTrue(high.getMessage().contains("1 to 5"));

        TripDtos.TripResponse rated = service.rateTrip(1L, 4);
        verify(tripRepository).save(trip);
        assertEquals(4, rated.rating());
    }

    @Test
    void getDailyStatsAggregatesTripRepository() {
        LocalDate day = LocalDate.of(2030, 1, 15);
        when(tripRepository.countByCreatedAtBetween(any(), any())).thenReturn(4L);
        when(tripRepository.avgPriceBetween(any(), any())).thenReturn(BigDecimal.valueOf(123.456));

        TripDtos.DailyStatsResponse stats = service.getDailyStats(day);

        assertEquals(4L, stats.tripsCount());
        assertEquals(BigDecimal.valueOf(123.46).setScale(2, RoundingMode.HALF_UP), stats.averagePrice());

        Instant start = day.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        verify(tripRepository).countByCreatedAtBetween(start, end);
        verify(tripRepository).avgPriceBetween(start, end);
    }

    @Test
    void createTripQueuesNotificationIncludingPassengerAndDriverIds() {
        when(rabbitTemplate.convertSendAndReceive(eq(""), eq(RabbitConfig.Q_PASSENGER_EXISTS), any(Map.class)))
                .thenReturn(Map.of("exists", true));
        when(rabbitTemplate.convertSendAndReceive(eq(""), eq(RabbitConfig.Q_DRIVER_ASSIGN), any(Map.class)))
                .thenReturn(Map.of("driverId", 42L));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripDtos.CreateTripRequest request = new TripDtos.CreateTripRequest(77L, "O", "D", 3.0);
        service.createTrip(request);

        verify(rabbitTemplate).convertAndSend(eq(""), eq(RabbitConfig.Q_NOTIFICATION_CREATE),
                argThat((NotificationMessage m) ->
                        m.passengerId().equals(77L)
                                && m.driverId().equals(42L)
                                && TripStatus.DRIVER_ASSIGNED.name().equals(m.status())));
    }

    @Test
    void createTripRequiresDistanceKm() {
        when(rabbitTemplate.convertSendAndReceive(eq(""), eq(RabbitConfig.Q_PASSENGER_EXISTS), any(Map.class)))
                .thenReturn(Map.of("exists", true));
        when(rabbitTemplate.convertSendAndReceive(eq(""), eq(RabbitConfig.Q_DRIVER_ASSIGN), any(Map.class)))
                .thenReturn(Map.of("driverId", 1L));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createTrip(new TripDtos.CreateTripRequest(1L, "A", "B", null)));
        assertTrue(ex.getMessage().contains("distance_km"));
    }

    @Test
    void updateStatusCompletedMarksDriverAvailable() {
        Trip trip = new Trip();
        trip.setPassengerId(2L);
        trip.setDriverId(11L);
        trip.setStatus(TripStatus.STARTED);
        when(tripRepository.findById(3L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateStatus(3L, TripStatus.COMPLETED);

        verify(rabbitTemplate).convertAndSend(eq(""), eq(RabbitConfig.Q_DRIVER_STATUS_UPDATE),
                eq(Map.of("driverId", 11L, "status", "AVAILABLE")));
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(eq(""), eq(RabbitConfig.Q_NOTIFICATION_CREATE), any(NotificationMessage.class));
    }

    @Test
    void updateStatusToAcceptedSetsDriverBusy() {
        Trip trip = new Trip();
        trip.setPassengerId(2L);
        trip.setDriverId(9L);
        trip.setStatus(TripStatus.DRIVER_ASSIGNED);
        when(tripRepository.findById(5L)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripDtos.TripResponse response = service.updateStatus(5L, TripStatus.ACCEPTED);

        assertEquals(TripStatus.ACCEPTED, response.status());
        verify(rabbitTemplate).convertAndSend(eq(""), eq(RabbitConfig.Q_DRIVER_STATUS_UPDATE),
                eq(Map.of("driverId", 9L, "status", "BUSY")));
        verify(rabbitTemplate).convertAndSend(eq(""), eq(RabbitConfig.Q_NOTIFICATION_CREATE), any(NotificationMessage.class));
    }
}

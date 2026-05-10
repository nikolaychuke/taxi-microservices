package api;

import com.fasterxml.jackson.annotation.JsonAlias;
import domain.TripStatus;

import java.math.BigDecimal;

public class TripDtos {
    public record CreateTripRequest(@JsonAlias("passenger_id") Long passengerId, String origin, String destination, @JsonAlias("distance_km") Double distanceKm) {}
    public record TripResponse(Long id, Long passengerId, Long driverId, TripStatus status, String origin, String destination, Double distanceKm, BigDecimal price, Integer rating) {}
    public record TripStatusUpdateRequest(TripStatus status) {}
    public record TripRatingRequest(Integer rating) {}
    public record DailyStatsResponse(Long tripsCount, BigDecimal averagePrice) {}
}

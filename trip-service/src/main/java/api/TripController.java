package org.example.taxi.trip.api;

import org.example.taxi.trip.domain.TripStatus;
import org.example.taxi.trip.service.TripManagementService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
public class TripController {
    private final TripManagementService service;

    public TripController(TripManagementService service) {
        this.service = service;
    }

    @PostMapping("/trips")
    public TripDtos.TripResponse createTrip(@RequestBody TripDtos.CreateTripRequest request) {
        return service.createTrip(request);
    }

    @GetMapping("/trips/{id}")
    public TripDtos.TripResponse getTrip(@PathVariable Long id) {
        return service.getTrip(id);
    }

    @GetMapping("/trips")
    public List<TripDtos.TripResponse> getTripHistory(@RequestParam("passenger_id") Long passengerId) {
        return service.getTripsByPassenger(passengerId);
    }

    @PatchMapping("/trips/{id}/status")
    public TripDtos.TripResponse updateStatus(@PathVariable Long id, @RequestBody TripDtos.TripStatusUpdateRequest request) {
        return service.updateStatus(id, request.status());
    }

    @PatchMapping("/trips/{id}/rating")
    public TripDtos.TripResponse rateTrip(@PathVariable Long id, @RequestBody TripDtos.TripRatingRequest request) {
        return service.rateTrip(id, request.rating());
    }

    @GetMapping("/trips/stats/daily")
    public TripDtos.DailyStatsResponse getDailyStats(@RequestParam(value = "date", required = false) LocalDate date) {
        return service.getDailyStats(date);
    }
}

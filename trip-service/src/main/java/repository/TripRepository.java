package repository;

import domain.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByPassengerIdOrderByCreatedAtDesc(Long passengerId);

    long countByCreatedAtBetween(Instant start, Instant end);

    @Query("select coalesce(avg(t.price), 0) from Trip t where t.createdAt between :start and :end")
    BigDecimal avgPriceBetween(@Param("start") Instant start, @Param("end") Instant end);
}

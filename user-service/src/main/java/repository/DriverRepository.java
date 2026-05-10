package org.example.taxi.user.repository;

import org.example.taxi.user.domain.Driver;
import org.example.taxi.user.domain.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    @Query(value = "select * from drivers d where d.status = 'AVAILABLE' order by d.id asc limit 1 for update", nativeQuery = true)
    Optional<Driver> findFirstAvailableForUpdate();

    @Query(value = "select * from drivers d where d.id = :id for update", nativeQuery = true)
    Optional<Driver> findByIdForUpdate(@Param("id") Long id);

    List<Driver> findTop20ByStatusOrderByIdAsc(DriverStatus status);
}

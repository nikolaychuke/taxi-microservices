package org.example.taxi.trip.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "passengers")
public class PassengerRef {
    @Id
    private Long id;
}

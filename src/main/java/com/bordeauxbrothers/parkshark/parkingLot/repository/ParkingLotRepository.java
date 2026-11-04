package com.bordeauxbrothers.parkshark.parkingLot.repository;

import com.bordeauxbrothers.parkshark.parkingLot.domain.ParkingLot;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParkingLotRepository extends JpaRepository<ParkingLot, Integer> {
    boolean existsByName(String name);
    boolean existsById(int id);

    @EntityGraph(attributePaths = {"division", "contactPerson", "address"})
    Optional<ParkingLot> findById(int id);
}

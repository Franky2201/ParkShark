package com.bordeauxbrothers.parkshark.parkingLot.repository;

import com.bordeauxbrothers.parkshark.parkingLot.domain.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingLotRepository extends JpaRepository<ParkingLot, Integer> {
}

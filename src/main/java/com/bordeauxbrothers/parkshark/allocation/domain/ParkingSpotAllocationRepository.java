package com.bordeauxbrothers.parkshark.allocation.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingSpotAllocationRepository extends JpaRepository<ParkingSpotAllocation, Integer> {
    List<ParkingSpotAllocation> findByParkingLotId(int parkingLotId);
}

package com.bordeauxbrothers.parkshark.allocation.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingSpotAllocationRepository extends JpaRepository<ParkingSpotAllocation, Integer> {
    List<ParkingSpotAllocation> findByParkingLotId(int parkingLotId);
    List<ParkingSpotAllocation> findAllBy(Pageable pageable);

    List<ParkingSpotAllocation> findByStatus(
            AllocationStatus status,
            Pageable pageable
    );
}

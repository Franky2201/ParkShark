package com.bordeauxbrothers.parkshark.allocation.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingSpotAllocationRepository extends JpaRepository<ParkingSpotAllocation, Integer> {
}

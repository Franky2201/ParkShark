package com.bordeauxbrothers.parkshark.allocation.service;

import com.bordeauxbrothers.parkshark.allocation.api.ParkingSpotAllocationMapper;
import com.bordeauxbrothers.parkshark.allocation.api.dto.AllocationDto;
import com.bordeauxbrothers.parkshark.allocation.api.dto.StartAllocationDto;
import com.bordeauxbrothers.parkshark.allocation.domain.ParkingSpotAllocationRepository;
import com.bordeauxbrothers.parkshark.allocation.exception.ParkingLotNotFoundException;
import com.bordeauxbrothers.parkshark.common.exception.BadRequestException;
import com.bordeauxbrothers.parkshark.parkingLot.repository.ParkingLotRepository;
import com.bordeauxbrothers.parkshark.person.domain.Member;
import com.bordeauxbrothers.parkshark.person.domain.Person;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Membership;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class ParkingSpotAllocationService {

    private final Clock clock;

    private final ParkingLotRepository parkingLotRepository;
    private final ParkingSpotAllocationRepository parkingSpotAllocationRepository;
    private final ParkingSpotAllocationMapper parkingSpotAllocationMapper;


    public ParkingSpotAllocationService(Clock clock,
            ParkingLotRepository parkingLotRepository, ParkingSpotAllocationRepository parkingSpotAllocationRepository, ParkingSpotAllocationMapper parkingSpotAllocationMapper) {
        this.clock = clock;
        this.parkingLotRepository = parkingLotRepository;
        this.parkingSpotAllocationRepository = parkingSpotAllocationRepository;
        this.parkingSpotAllocationMapper = parkingSpotAllocationMapper;
    }

    public AllocationDto startAllocatingParkingSpot(@Valid StartAllocationDto startAllocationDto, Person user) {
        if (!(user instanceof Member member))
            throw new BadRequestException("User is not a member and can't therefore be allocated a parking spot");
        if (member.getMembership() != Membership.GOLD &&
                !(member.getLicensePlate().equals(startAllocationDto.licensePlate()))) {
            throw new IllegalArgumentException("Licence plate has to match member if membership is not Gold");
        }
        if (!parkingLotRepository.existsById(startAllocationDto.parkingLotId()))
            throw new ParkingLotNotFoundException(startAllocationDto.parkingLotId());

        OffsetDateTime startTime = OffsetDateTime.now(clock);
        return parkingSpotAllocationMapper.toDto(
                parkingSpotAllocationRepository.save(
                        parkingSpotAllocationMapper.toEntity(startAllocationDto, member, startTime)));
    }
}
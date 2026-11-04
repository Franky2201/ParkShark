package com.bordeauxbrothers.parkshark.allocation.service;

import com.bordeauxbrothers.parkshark.allocation.api.ParkingSpotAllocationMapper;
import com.bordeauxbrothers.parkshark.allocation.api.dto.AllocationDto;
import com.bordeauxbrothers.parkshark.allocation.api.dto.StartAllocationDto;
import com.bordeauxbrothers.parkshark.allocation.domain.AllocationStatus;
import com.bordeauxbrothers.parkshark.allocation.domain.ParkingSpotAllocationRepository;
import com.bordeauxbrothers.parkshark.common.exception.BadRequestException;
import com.bordeauxbrothers.parkshark.common.exception.ConflictException;
import com.bordeauxbrothers.parkshark.common.exception.ResourceNotFoundException;
import com.bordeauxbrothers.parkshark.parkingLot.domain.ParkingLot;
import com.bordeauxbrothers.parkshark.parkingLot.repository.ParkingLotRepository;
import com.bordeauxbrothers.parkshark.person.domain.Member;
import com.bordeauxbrothers.parkshark.person.domain.Person;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Membership;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class ParkingSpotAllocationService {

    private final Clock clock;

    private final ParkingSpotAllocationRepository parkingSpotAllocationRepository;
    private final ParkingSpotAllocationMapper parkingSpotAllocationMapper;
    private final ParkingLotRepository parkingLotRepository;


    public ParkingSpotAllocationService(Clock clock, ParkingSpotAllocationRepository parkingSpotAllocationRepository, ParkingSpotAllocationMapper parkingSpotAllocationMapper, ParkingLotRepository parkingLotRepository) {
        this.clock = clock;
        this.parkingSpotAllocationRepository = parkingSpotAllocationRepository;
        this.parkingSpotAllocationMapper = parkingSpotAllocationMapper;
        this.parkingLotRepository = parkingLotRepository;
    }

    @Transactional
    public AllocationDto startAllocatingParkingSpot(@Valid StartAllocationDto startAllocationDto, Person user) {
        //Member member = (Member) user;
        Member member = user instanceof Member ? ((Member) user) : null;
        assert member != null;
        if (member.getMembership() != Membership.GOLD &&
                !(member.getLicensePlate().getNumber().equals(startAllocationDto.licencePlate().getNumber()))) {
            throw new BadRequestException("Licence plate has to match member if membership is not Gold");
        }
        int maxCapacity = parkingLotRepository.findById(startAllocationDto.parkingLotId()).map(ParkingLot::getMaxCapacity)
                .orElseThrow(() -> new ResourceNotFoundException("Parking Lot does not exist"));


        int usedCapacity = Math.toIntExact(parkingSpotAllocationRepository.findByParkingLotId(startAllocationDto.parkingLotId())
                .stream()
                .filter(parkingSpotAllocation -> parkingSpotAllocation.getStatus() == AllocationStatus.ACTIVE)
                .count());

        if (usedCapacity >= maxCapacity) {
            throw new ConflictException("Parking Lot has not enough capacity left");
        }

        OffsetDateTime startTime = OffsetDateTime.now(clock);
        return parkingSpotAllocationMapper.toDto(
                parkingSpotAllocationRepository.save(
                        parkingSpotAllocationMapper.toEntity(startAllocationDto, member, startTime)));
    }
}

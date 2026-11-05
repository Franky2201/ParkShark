package com.bordeauxbrothers.parkshark.allocation.service;

import com.bordeauxbrothers.parkshark.allocation.api.dto.AllocationDto;
import com.bordeauxbrothers.parkshark.allocation.api.dto.StartAllocationDto;
import com.bordeauxbrothers.parkshark.allocation.domain.AllocationStatus;
import com.bordeauxbrothers.parkshark.allocation.domain.ParkingSpotAllocation;
import com.bordeauxbrothers.parkshark.allocation.domain.ParkingSpotAllocationRepository;
import com.bordeauxbrothers.parkshark.allocation.exception.ParkingLotNotFoundException;
import com.bordeauxbrothers.parkshark.common.exception.BadRequestException;
import com.bordeauxbrothers.parkshark.common.exception.ConflictException;
import com.bordeauxbrothers.parkshark.common.exception.ResourceNotFoundException;
import com.bordeauxbrothers.parkshark.common.security.exception.ForbiddenException;
import com.bordeauxbrothers.parkshark.parkingLot.domain.ParkingLot;
import com.bordeauxbrothers.parkshark.parkingLot.repository.ParkingLotRepository;
import com.bordeauxbrothers.parkshark.person.domain.Member;
import com.bordeauxbrothers.parkshark.person.domain.Person;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Membership;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;


@Service
public class ParkingSpotAllocationService {

    private final Clock clock;
    Logger log = LoggerFactory.getLogger(ParkingSpotAllocationService.class);
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
        if (!(user instanceof Member member)) {
            throw new ForbiddenException(
                    "Only members can start a parking allocation"
            );
        }
        log.info("Checking if member has the right licence plate");
        if (member.getMembership() != Membership.GOLD &&
                !(member.getLicensePlate().getNumber().equals(startAllocationDto.licensePlate().getNumber()))) {
            throw new BadRequestException("Licence plate has to match member if membership is not Gold");
        }

        log.info("Checking if parking lot {} has enough capacity", startAllocationDto.parkingLotId());
        int maxCapacity = parkingLotRepository.findById(startAllocationDto.parkingLotId()).map(ParkingLot::getMaxCapacity)
                .orElseThrow(() -> new ParkingLotNotFoundException(startAllocationDto.parkingLotId()));


        int usedCapacity = Math.toIntExact(parkingSpotAllocationRepository.findByParkingLotId(startAllocationDto.parkingLotId())
                .stream()
                .filter(parkingSpotAllocation -> parkingSpotAllocation.getStatus() == AllocationStatus.ACTIVE)
                .count());

        if (usedCapacity >= maxCapacity) {
            throw new ConflictException("Parking Lot has not enough capacity left");
        }

        OffsetDateTime startTime = OffsetDateTime.now(clock);
        OffsetDateTime referenceTime = OffsetDateTime.now(clock);
        return parkingSpotAllocationMapper.toDto(
                parkingSpotAllocationRepository.save(
                        parkingSpotAllocationMapper.toEntity(startAllocationDto, member, startTime)),
                referenceTime);
    }

    @Transactional
    public AllocationDto stopAllocatingParkingSpot(int id, Person user) {
        if (!(user instanceof Member member)) {
            throw new ForbiddenException(
                    "Only members can stop a parking allocation"
            );
        }
        ParkingSpotAllocation allocation = parkingSpotAllocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation does not exist"));
        if (!(allocation.getMemberId().equals(member.getId()))) {
            throw new ForbiddenException("Only member that created Allocation, can stop it");
        }
        if (allocation.getStatus() != AllocationStatus.ACTIVE) {
            throw new ConflictException("Allocation has to be active to be stopped");
        }
        
        OffsetDateTime endTime = OffsetDateTime.now(clock);
        allocation.stop(endTime);
        log.info("Allocation {} stopped at {}", id, endTime);
        return parkingSpotAllocationMapper.toDto(parkingSpotAllocationRepository.save(allocation), endTime);
    }

    @Transactional(readOnly = true)
    public List<AllocationDto> getAllAllocatingParkingSpotsFiltered(Integer limit, AllocationStatus status, boolean desc) {
        log.info("Getting all allocating parking spots");
        if (limit != null && limit <= 0) {
            throw new BadRequestException(
                    "Limit must be greater than zero"
            );
        }

        Sort.Direction direction = (desc)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        log.debug("Sorting by startTime and id");
        Sort sort = Sort.by(direction, "startTime")
                .and(Sort.by(direction, "id"));

        Pageable pageable = (limit == null)
                ? Pageable.unpaged(sort)
                : PageRequest.of(0, limit, sort);

        List<ParkingSpotAllocation> allocations = (status == null)
                ? parkingSpotAllocationRepository.findAllBy(pageable)
                : parkingSpotAllocationRepository.findByStatus(status, pageable);

        OffsetDateTime now = OffsetDateTime.now(clock);
        return allocations.stream()
                .map(allocation -> parkingSpotAllocationMapper.toDto(allocation, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AllocationDto> getAllAllocatingParkingSpotsForMemberFiltered(int id, AllocationStatus status) {
        log.info("Getting all allocating parking spots for member {}", id);
        List<ParkingSpotAllocation> allocations = (status == null)
                ? parkingSpotAllocationRepository.findAllByMemberId(id)
                : parkingSpotAllocationRepository.findAllByMemberIdAndStatus(id, status);

        OffsetDateTime now = OffsetDateTime.now(clock);
        return allocations.stream()
                .map(allocation -> parkingSpotAllocationMapper.toDto(allocation, now))
                .toList();
    }

    public List<AllocationDto> getAllAllocatingParkingSpotsForParkingLotFiltered(int id, AllocationStatus status) {
        log.info("Getting all allocating parking spots for parking lot {}", id);
        List<ParkingSpotAllocation> allocations = (status == null)
                ? parkingSpotAllocationRepository.findByParkingLotId(id)
                : parkingSpotAllocationRepository.findAllByParkingLotIdAndStatus(id, status);

        OffsetDateTime now = OffsetDateTime.now(clock);
        return allocations.stream()
                .map(allocation -> parkingSpotAllocationMapper.toDto(allocation, now))
                .toList();
    }
}

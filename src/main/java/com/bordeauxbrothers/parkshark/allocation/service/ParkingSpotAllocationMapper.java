package com.bordeauxbrothers.parkshark.allocation.service;

import com.bordeauxbrothers.parkshark.allocation.api.dto.AllocationDto;
import com.bordeauxbrothers.parkshark.allocation.api.dto.StartAllocationDto;
import com.bordeauxbrothers.parkshark.allocation.domain.ParkingSpotAllocation;
import com.bordeauxbrothers.parkshark.person.domain.Member;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;

@Component
public class ParkingSpotAllocationMapper {
    Logger logger = LoggerFactory.getLogger(ParkingSpotAllocationMapper.class);
    public ParkingSpotAllocation toEntity(@Valid StartAllocationDto startAllocationDto, Member member, OffsetDateTime startTime) {
        logger.debug("Mapping StartAllocationDto to ParkingSpotAllocation");
        return new ParkingSpotAllocation(member.getId(),
                startAllocationDto.parkingLotId(),
                startAllocationDto.licensePlate().number(),
                startTime);
    }

    public AllocationDto toDto(ParkingSpotAllocation allocation, OffsetDateTime referenceTime) {
        logger.debug("Mapping ParkingSpotAllocation to AllocationDto");
        return new AllocationDto(
                allocation.getId(),
                allocation.getMemberId(),
                allocation.getParkingLotId(),
                allocation.getLicensePlateNumber(),
                allocation.getStartTime(),
                allocation.getEndTime(),
                formatDuration(allocation.getDuration(referenceTime)),
                allocation.getStatus()
        );
    }

    private String formatDuration(Duration duration) {
        return String.format("%02d:%02d:%02d",
                duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
    }
}
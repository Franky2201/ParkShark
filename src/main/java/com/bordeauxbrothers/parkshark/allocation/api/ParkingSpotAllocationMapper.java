package com.bordeauxbrothers.parkshark.allocation.api;

import com.bordeauxbrothers.parkshark.allocation.api.dto.AllocationDto;
import com.bordeauxbrothers.parkshark.allocation.api.dto.StartAllocationDto;
import com.bordeauxbrothers.parkshark.allocation.domain.ParkingSpotAllocation;
import com.bordeauxbrothers.parkshark.person.domain.Member;
import jakarta.validation.Valid;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class ParkingSpotAllocationMapper {
    public ParkingSpotAllocation toEntity(@Valid StartAllocationDto startAllocationDto, Member member, OffsetDateTime startTime) {
        return new ParkingSpotAllocation(member.getId(),
                startAllocationDto.parkingLotId(),
                startAllocationDto.licencePlate().getNumber(),
                startTime);
    }

    public AllocationDto toDto(ParkingSpotAllocation allocation) {
        return new AllocationDto(
                allocation.getId(),
                allocation.getMemberId(),
                allocation.getParkingLotId(),
                allocation.getLicensePlateNumber(),
                allocation.getStartTime(),
                allocation.getEndTime(),
                allocation.getStatus()
        );
    }
}

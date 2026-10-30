package com.bordeauxbrothers.parkshark.allocation.api.dto;

import com.bordeauxbrothers.parkshark.allocation.domain.AllocationStatus;

import java.time.OffsetDateTime;

public record AllocationDto(
        Integer id,
        Integer memberId,
        Integer parkingLotId,
        String licensePlateNumber,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        AllocationStatus status) {
}

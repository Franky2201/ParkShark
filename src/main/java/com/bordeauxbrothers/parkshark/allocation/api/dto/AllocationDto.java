package com.bordeauxbrothers.parkshark.allocation.api.dto;

import com.bordeauxbrothers.parkshark.allocation.domain.AllocationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;
import java.time.OffsetDateTime;

public record AllocationDto(
        Integer id,
        Integer memberId,
        Integer parkingLotId,
        String licensePlateNumber,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        @Schema(description = "Duration of the allocation", example = "02:20:15")
        String duration,
        AllocationStatus status) {
}

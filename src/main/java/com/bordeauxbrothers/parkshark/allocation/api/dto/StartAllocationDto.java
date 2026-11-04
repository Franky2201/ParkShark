package com.bordeauxbrothers.parkshark.allocation.api.dto;

import com.bordeauxbrothers.parkshark.person.domain.persondetails.LicensePlate;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record StartAllocationDto(
        @NotNull(message = "an allocation needs a licence plate")
        LicensePlate licencePlate,
        @NotNull(message = "an allocation needs a parking lot linked")
        Integer parkingLotId
) {
}
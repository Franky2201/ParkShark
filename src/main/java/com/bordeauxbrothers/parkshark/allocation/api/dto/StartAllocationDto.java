package com.bordeauxbrothers.parkshark.allocation.api.dto;

import com.bordeauxbrothers.parkshark.person.domain.persondetails.LicensePlate;
import jakarta.validation.constraints.NotEmpty;

public record StartAllocationDto(
        @NotEmpty(message = "an allocation needs a licence plate")
        LicensePlate licencePlate,
        @NotEmpty(message = "an allocation needs a parking lot linked")
        int parkingLotId
) {
}

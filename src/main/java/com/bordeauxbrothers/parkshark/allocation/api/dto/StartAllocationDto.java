package com.bordeauxbrothers.parkshark.allocation.api.dto;

import com.bordeauxbrothers.parkshark.person.api.dtos.LicensePlateDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Information required to start a parking session.")
public record StartAllocationDto(

        @Schema(
                description = """
                        Licence plate of the actual car being parked.
                        Must match the member's registered number unless the member has Gold membership.
                        """
        )
        @NotNull(message = "An allocation needs a licence plate")
        @Valid
        LicensePlateDTO licensePlate,

        @Schema(
                description = "ID of an existing parking lot with available capacity.",
                example = "42"
        )
        @NotNull(message = "An allocation needs a parking lot")
        @Positive(message = "Parking lot ID must be greater than zero")
        Integer parkingLotId
) {
}
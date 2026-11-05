package com.bordeauxbrothers.parkshark.person.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "A postal code with its label (e.g. 3000, Leuven).")
public record PostalCodeDTO(

        @Schema(description = "The actual postal code.", example = "3000")
        @NotBlank(message = "zip code should not be blank")
        @JsonProperty("zip_code")
        String zipCode,

        @Schema(description = "Name of the municipality the postal code belongs to.", example = "Leuven")
        @NotBlank(message = "postal code label should not be blank")
        String label
) {
}
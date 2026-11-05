package com.bordeauxbrothers.parkshark.person.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "An address: street, number and postal code.")
public record AddressDTO(

        @Schema(description = "Name of the street.", example = "Bondgenotenlaan")
        @NotBlank(message = "street should not be blank")
        String street,

        @Schema(description = "House or building number, may include a suffix.", example = "12")
        @NotBlank(message = "number should not be blank")
        String number,

        @Schema(description = "Postal code and its label.")
        @NotNull(message = "postal code should not be null")
        @Valid
        @JsonProperty("postal_code")
        PostalCodeDTO postalCodeDTO
) {
}
package com.bordeauxbrothers.parkshark.person.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "An address: street, number and postal code.")
public class AddressDTO {

    @Schema(description = "Name of the street.", example = "Bondgenotenlaan")
    @JsonProperty("street")
    @NotBlank(message = "street should not be blank")
    private String street;

    @Schema(description = "House or building number, may include a suffix.", example = "12")
    @JsonProperty("number")
    @NotBlank(message = "number should not be blank")
    private String number;

    @Schema(description = "Postal code and its label.")
    @NotNull(message = "postal code should not be null")
    @Valid
    @JsonProperty("postal_code")
    private PostalCodeDTO postalCodeDTO;

    public AddressDTO(String street, String number, PostalCodeDTO postalCodeDTO) {
        this.street = street;
        this.number = number;
        this.postalCodeDTO = postalCodeDTO;
    }

    public String getStreet() {
        return street;
    }

    public String getNumber() {
        return number;
    }

    public PostalCodeDTO getPostalCodeDTO() {
        return postalCodeDTO;
    }
}
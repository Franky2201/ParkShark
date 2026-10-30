package com.bordeauxbrothers.parkshark.person.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AddressDTO {
    @JsonProperty("street")
    @NotBlank(message = "street should not be blank")
    private String street;
    @JsonProperty("number")
    @NotBlank(message = "number should not be blank")
    private String number;
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

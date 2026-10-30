package com.bordeauxbrothers.parkshark.person.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class PostalCodeDTO {
    @NotBlank(message = "zip code should not be blank")
    @JsonProperty("zip_code")
    private String zipCode;
    @NotBlank(message = "country label should not be blank")
    @JsonProperty("label")
    private String label;

    public PostalCodeDTO(String zipCode, String label) {
        this.zipCode = zipCode;
        this.label = label;
    }
    public String getZipCode() {
        return zipCode;
    }
    public String getLabel() {
        return label;
    }
}

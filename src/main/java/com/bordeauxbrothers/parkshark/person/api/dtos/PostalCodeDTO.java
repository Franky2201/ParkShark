package com.bordeauxbrothers.parkshark.person.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "A postal code with its label (e.g. 3000, Leuven).")
public class PostalCodeDTO {

    @Schema(description = "The actual postal code.", example = "3000")
    @NotBlank(message = "zip code should not be blank")
    @JsonProperty("zip_code")
    private String zipCode;

    @Schema(description = "Name of the municipality the postal code belongs to.", example = "Leuven")
    @NotBlank(message = "postal code label should not be blank")
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
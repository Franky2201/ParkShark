package com.bordeauxbrothers.parkshark.person.service.dtos;

import jakarta.validation.constraints.NotBlank;

public class LicensePlateDTO {
    @NotBlank
    private String number;
    @NotBlank
    private String country;

    public LicensePlateDTO(String number, String country) {
        this.number = number;
        this.country = country;
    }
    public String getNumber() {
        return number;
    }
    public String getCountry() {
        return country;
    }
}

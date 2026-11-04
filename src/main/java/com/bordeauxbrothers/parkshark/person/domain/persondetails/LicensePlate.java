package com.bordeauxbrothers.parkshark.person.domain.persondetails;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
public class LicensePlate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotBlank(message = "A licence plate number is required")
    @Size(max = 30, message = "A licence plate number may contain at most 30 characters")
    @Column(name = "number")
    private String number;
    @Column(name = "country")
    private String country;

    public LicensePlate(String number, String country) {
        this.number = number;
        this.country = country;
    }
    protected LicensePlate() {
    }

    public String getNumber() {
        return number;
    }
    public String getCountry() {
        return country;
    }
    public Integer getId() {
        return id;
    }
}

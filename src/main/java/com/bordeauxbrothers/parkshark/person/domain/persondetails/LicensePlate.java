package com.bordeauxbrothers.parkshark.person.domain.persondetails;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LicensePlate that = (LicensePlate)o;
        return Objects.equals(getNumber(), that.getNumber()) && Objects.equals(getCountry(), that.getCountry());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNumber(), getCountry());
    }
}
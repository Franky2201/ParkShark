package com.bordeauxbrothers.parkshark.person.domain.persondetails;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
public class LicensePlate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
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
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
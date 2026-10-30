package com.bordeauxbrothers.parkshark.person.domain.persondetails;

import jakarta.persistence.*;

@Entity
public class PostalCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "zip_code")
    private String zipCode;
    @Column(name = "label")
    private String label;

    public PostalCode(String zipCode, String countryLabel) {
        this.zipCode = zipCode;
        this.label = countryLabel;
    }

    protected PostalCode() {
    }

    public String getZipCode() {
        return zipCode;
    }
    public String getLabel() {
        return label;
    }
    public Integer getId() {
        return id;
    }
}

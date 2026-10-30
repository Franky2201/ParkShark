package com.bordeauxbrothers.parkshark.person.domain.persondetails;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
public class PostalCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotBlank(message = "zip code should not be blank")
    @Column(name = "zip_code")
    private String zipCode;
    @NotBlank(message = "country label should not be blank")
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

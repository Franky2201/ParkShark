package com.bordeauxbrothers.parkshark.person.domain.persondetails;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotBlank(message = "street should not be blank")
    @Column(name = "street")
    private String street;
    @NotBlank(message = "number should not be blank")
    @Column(name = "number")
    private String number;
    @ManyToOne
    @JoinColumn(name = "postal_code_id")
    @NotNull(message = "postal code should not be null")
    @Valid
    private PostalCode postalCode;

    public Address(String street, String number, PostalCode postalCode) {
        this.street = street;
        this.number = number;
        this.postalCode = postalCode;
    }

    protected Address() {
    }

    public Integer getId() {
        return id;
    }

    public String getStreet() {
        return street;
    }

    public String getNumber() {
        return number;
    }

    public PostalCode getPostalCode() {
        return postalCode;
    }
}

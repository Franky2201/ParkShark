package com.bordeauxbrothers.parkshark.person.domain.persondetails;

import jakarta.persistence.*;

@Entity
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "street")
    private String street;
    @Column(name = "number")
    private String number;
    @ManyToOne
    @JoinColumn(name = "postal_code_id")
    private PostalCode postalCode;

    public Address(String street, String number, PostalCode postalCode) {
        this.street = street;
        this.number = this.number;
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

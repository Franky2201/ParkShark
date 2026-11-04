package com.bordeauxbrothers.parkshark.parkingLot.domain;

import com.bordeauxbrothers.parkshark.division.domain.Division;
import com.bordeauxbrothers.parkshark.person.domain.Member;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Address;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
public class ParkingLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private Category category;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Column(name = "price_per_hour_in_euro")
    private BigDecimal pricePerHour;

    @ManyToOne
    @JoinColumn(name = "contact_person_id")
    private Member contactPerson;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "address_id")
    private Address address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "division_id")
    private Division division;

    public ParkingLot(String name, Category category, Integer maxCapacity, BigDecimal pricePerHour,
            Member contactPerson, Address address, Division division) {
        this.name = name;
        this.category = category;
        this.maxCapacity = maxCapacity;
        this.pricePerHour = pricePerHour;
        this.contactPerson = contactPerson;
        this.address = address;
        this.division = division;
    }

    protected ParkingLot() {
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    public Member getContactPerson() {
        return contactPerson;
    }

    public Address getAddress() {
        return address;
    }

    public Division getDivision() {
        return division;
    }
}
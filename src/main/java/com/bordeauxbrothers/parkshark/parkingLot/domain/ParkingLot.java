package com.bordeauxbrothers.parkshark.parkingLot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class ParkingLot {

    @Id
    private Integer id;
/*
    @Column(name = "name")
    private Name name;

    @Column(name = "category")
    private Category category;

    @Column(name = "max_capacity")
    private Integer capacity;

    @Column(name = "")
*/
    public ParkingLot() {
    }
}
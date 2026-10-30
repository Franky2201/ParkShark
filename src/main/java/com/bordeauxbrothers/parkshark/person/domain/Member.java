package com.bordeauxbrothers.parkshark.person.domain;

import com.bordeauxbrothers.parkshark.person.domain.persondetails.*;
import jakarta.persistence.*;

import java.time.LocalDate;
@Entity
@DiscriminatorValue("USER")
public class Member extends Person {

    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address address;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @ManyToOne
    @JoinColumn(name = "license_plate_id")
    private LicensePlate licensePlate;

    @Enumerated(EnumType.STRING)
    private Membership membership = Membership.BRONZE;

    protected Member() {
    }

    public Member(
            Name name,
            String email,
            String passwordHash,
            LocalDate registrationDate,
            Address address,
            String phoneNumber,
            String mobileNumber,
            LicensePlate licensePlate,
            Membership membership) {

        super(name, email, passwordHash, registrationDate, Role.USER);

        this.address = address;
        this.phoneNumber = phoneNumber;
        this.mobileNumber = mobileNumber;
        this.licensePlate = licensePlate;
        this.membership = membership != null
                ? membership
                : Membership.BRONZE;
    }

    public Address getAddress() { return address; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getMobileNumber() { return mobileNumber; }
    public LicensePlate getLicensePlate() { return licensePlate; }
    public Membership getMembership() { return membership; }
    }

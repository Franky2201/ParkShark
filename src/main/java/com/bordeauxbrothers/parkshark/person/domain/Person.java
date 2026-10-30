package com.bordeauxbrothers.parkshark.person.domain;

import com.bordeauxbrothers.parkshark.person.domain.persondetails.Feature;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Membership;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Name;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Role;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public abstract class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Embedded
    private Name name;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership")
    private Membership membership;

    protected Person() {
    }

    protected Person(
            Name name,
            String email,
            String passwordHash,
            LocalDate registrationDate,
            Role role) {

        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.registrationDate = registrationDate;
        this.role = role;
    }

    public Integer getId() {
        return id;
    }

    public Name getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public Role getRole() {
        return role;
    }

    public boolean canHaveAccessTo(Feature feature){
        return this.role.hasFeature(feature);
    }
}
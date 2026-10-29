package com.bordeauxbrothers.parkshark.person.domain;

import com.bordeauxbrothers.parkshark.person.domain.persondetails.Name;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Role;
import jakarta.persistence.Entity;

import java.time.LocalDate;

@Entity
public class Manager extends Person{

    protected Manager() {}

    public Manager(
            Name name,
            String email,
            String passwordHash,
            LocalDate registrationDate
    ){
        super(name, email, passwordHash, registrationDate, Role.MANAGER);
    }
}

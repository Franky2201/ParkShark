package com.bordeauxbrothers.parkshark.person.domain;

import com.bordeauxbrothers.parkshark.person.domain.persondetails.Name;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Role;
import jakarta.persistence.Entity;

import java.time.LocalDate;

@Entity
public class Admin extends Person{

    public Admin(
            Name name,
            String email,
            String passwordHash,
            LocalDate registrationDate
    ){
        super(name, email,passwordHash, registrationDate, Role.ADMIN);
    }
    protected Admin() {
    }
}

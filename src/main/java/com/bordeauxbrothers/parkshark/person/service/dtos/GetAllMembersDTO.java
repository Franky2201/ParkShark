package com.bordeauxbrothers.parkshark.person.service.dtos;

import com.bordeauxbrothers.parkshark.person.domain.persondetails.Name;

import java.time.LocalDate;

public class GetAllMembersDTO {
    private final Integer id;
    private final Name name;
    private final String licensePlateNumber;
    private final String phoneNumber;
    private final String email;
    private final LocalDate registrationDate;

    public GetAllMembersDTO(Integer id, Name name, String licensePlateNumber, String phoneNumber, String email, LocalDate registrationDate) {
        this.id = id;
        this.name = name;
        this.licensePlateNumber = licensePlateNumber;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.registrationDate = registrationDate;
    }
    public Integer getId() {
        return id;
    }
    public Name getName() {
        return name;
    }
    public String getLicensePlateNumber() {
        return licensePlateNumber;
    }
    public String getPhoneNumber() {
        return phoneNumber;
    }
    public String getEmail() {
        return email;
    }
    public LocalDate getRegistrationDate() {
        return registrationDate;
    }
}

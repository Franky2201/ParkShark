package com.bordeauxbrothers.parkshark.person.service.dtos;

import com.bordeauxbrothers.parkshark.person.domain.persondetails.Address;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.LicensePlate;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Membership;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Name;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@AtLeastOnePhone
public class AddMemberDTO {
    @NotNull(message = "Name should not be null")
    @Valid
    private Name name;
    @NotNull(message = "Address should not be null")
    @Valid
    private Address address;
    private String phoneNumber;
    private String mobileNumber;
    @NotBlank(message = "Email should not be blank")
    @Email(message = "Email should be valid")
    @Pattern(
            regexp = "^[A-Za-z0-9.]+@[A-Za-z0-9]+\\.[A-Za-z0-9]+$",
            message = "Email must have the form x@x.x using only letters and numbers"
    )
    private String email;
    @NotBlank
    @Size(min = 6, message = "Password should be at least 6 characters long")
    private String password;
    @NotNull
    @Valid
    private LicensePlate licensePlate;
    private Membership membership;

    public AddMemberDTO(Name name, Address address, String phoneNumber, String mobileNumber, String email, String password, LicensePlate licensePlate, Membership membership) {
        this.name = name;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.mobileNumber = mobileNumber;
        this.email = email;
        this.password = password;
        this.licensePlate = licensePlate;
        this.membership = membership;
    }

    public Name getName() {
        return name;
    }

    public Address getAddress() {
        return address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public LicensePlate getLicensePlate() {
        return licensePlate;
    }

    public String getPassword() {
        return password;
    }
    public String getMobileNumber() {
        return mobileNumber;
    }
    public Membership getMembership() {
        return membership;
    }
}

package com.bordeauxbrothers.parkshark.person.service.dtos;

import com.bordeauxbrothers.parkshark.person.domain.persondetails.*;
import com.bordeauxbrothers.parkshark.person.service.customvalidators.AtLeastOnePhone;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@AtLeastOnePhone
public class AddMemberDTO {
    @NotNull(message = "Name should not be null")
    @Valid
    private Name name;
    @NotNull(message = "Address should not be null")
    @Valid
    @JsonProperty("address")
    private AddressDTO addressDTO;
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
    @JsonProperty("license_plate")
    private LicensePlateDTO licensePlateDTO;
    private Membership membership;

    public AddMemberDTO(Name name, AddressDTO addressDTO, String phoneNumber, String mobileNumber, String email, String password, LicensePlateDTO licensePlateDTO, Membership membership) {
        this.name = name;
        this.addressDTO = addressDTO;
        this.phoneNumber = phoneNumber;
        this.mobileNumber = mobileNumber;
        this.email = email;
        this.password = password;
        this.licensePlateDTO = licensePlateDTO;
        this.membership = membership;
    }

    public Name getName() {
        return name;
    }

    public AddressDTO getAddressDTO() {
        return addressDTO;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public LicensePlateDTO getLicensePlateDTO() {
        return licensePlateDTO;
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

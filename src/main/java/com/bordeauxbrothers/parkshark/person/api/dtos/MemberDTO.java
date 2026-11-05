package com.bordeauxbrothers.parkshark.person.api.dtos;

import com.bordeauxbrothers.parkshark.person.domain.persondetails.Membership;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Name;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "All data of a single member. Never contains the password.")
public record MemberDTO(

        @Schema(description = "Unique ID of the member.", example = "7")
        Integer id,

        @Schema(description = "First and last name of the member.")
        Name name,

        @Schema(description = "Home address of the member.")
        @JsonProperty("address")
        AddressDTO addressDTO,

        @Schema(description = "Telephone number of the member.", example = "+32 16 12 34 56")
        String phoneNumber,

        @Schema(description = "Mobile phone number of the member.", example = "+32 470 12 34 56")
        String mobileNumber,

        @Schema(description = "Email address of the member.", example = "tony@parkshark.be")
        String email,

        @Schema(description = "Licence plate of the member's car.")
        @JsonProperty("license_plate")
        LicensePlateDTO licensePlateDTO,

        @Schema(description = "Current membership level.", example = "SILVER")
        Membership membership,

        @Schema(description = "Date the member registered.", example = "2026-11-04")
        LocalDate registrationDate
) {
}
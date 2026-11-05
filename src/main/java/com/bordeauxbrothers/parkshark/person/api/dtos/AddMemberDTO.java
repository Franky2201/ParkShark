package com.bordeauxbrothers.parkshark.person.api.dtos;

import com.bordeauxbrothers.parkshark.person.domain.persondetails.*;
import com.bordeauxbrothers.parkshark.person.service.customvalidators.AtLeastOnePhone;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@Schema(description = "Information required to register a new member. At least one of phoneNumber or mobileNumber is required.")
@AtLeastOnePhone
public record AddMemberDTO(

        @Schema(description = "First and last name of the member.")
        @NotNull(message = "Name should not be null")
        @Valid
        Name name,

        @Schema(description = "Home address of the member.")
        @NotNull(message = "Address should not be null")
        @Valid
        @JsonProperty("address")
        AddressDTO addressDTO,

        @Schema(description = "Telephone number. Required if no mobile number is given.", example = "+32 16 12 34 56")
        String phoneNumber,

        @Schema(description = "Mobile phone number. Required if no telephone number is given.", example = "+32 470 12 34 56")
        String mobileNumber,

        @Schema(description = "Email address, of the form x@x.x. Must not already be in use.", example = "tony@parkshark.be")
        @NotBlank(message = "Email should not be blank")
        @Email(message = "Email should be valid")
        @Pattern(
                regexp = "^[A-Za-z0-9.]+@[A-Za-z0-9]+\\.[A-Za-z0-9]+$",
                message = "Email must have the form x@x.x using only letters and numbers"
        )
        String email,

        @Schema(
                description = "Password for Basic Authentication, at least 6 characters.",
                example = "secret123",
                format = "password",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "Password should not be blank")
        @Size(min = 6, message = "Password should be at least 6 characters long")
        String password,

        @Schema(description = "Licence plate of the member's car.")
        @NotNull(message = "License plate should not be null")
        @Valid
        @JsonProperty("license_plate")
        LicensePlateDTO licensePlateDTO,

        @Schema(description = "Membership level. Optional, defaults to BRONZE.", example = "SILVER")
        Membership membership
) {
    @Override
    public String toString() {
        return "AddMemberDTO[email=" + email + "]";
    }
}
package com.bordeauxbrothers.parkshark.person.api.dtos;

import com.bordeauxbrothers.parkshark.person.domain.persondetails.Name;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Short overview of a member.")
public record GetAllMembersDTO(

        @Schema(description = "Unique ID of the member.", example = "7")
        Integer id,

        @Schema(description = "First and last name of the member.")
        Name name,

        @Schema(description = "Licence plate number of the member's car.", example = "1-ABC-123")
        String licensePlateNumber,

        @Schema(description = "Telephone number of the member.", example = "+32 16 12 34 56")
        String phoneNumber,

        @Schema(description = "Email address of the member.", example = "tony@parkshark.be")
        String email,

        @Schema(description = "Date the member registered.", example = "2026-11-04")
        LocalDate registrationDate
) {
}
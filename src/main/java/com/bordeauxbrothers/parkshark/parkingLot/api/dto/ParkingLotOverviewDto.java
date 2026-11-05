package com.bordeauxbrothers.parkshark.parkingLot.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Short overview of a parking lot.")
public record ParkingLotOverviewDto(

        @Schema(description = "Unique ID of the parking lot.", example = "42")
        int id,

        @Schema(description = "Name of the parking lot.", example = "Shark Central Underground")
        String name,

        @Schema(description = "Maximum number of cars that can be parked at the same time.", example = "250")
        int capacity,

        @Schema(description = "Email address of the contact person.", example = "tony@parkshark.be")
        String contactPersonEmail,

        @Schema(description = "Telephone number of the contact person.", example = "+32 16 12 34 56")
        String contactPersonTelephone
) {
}
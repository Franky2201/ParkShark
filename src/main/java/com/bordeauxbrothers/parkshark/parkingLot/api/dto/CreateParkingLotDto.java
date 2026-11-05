package com.bordeauxbrothers.parkshark.parkingLot.api.dto;

import com.bordeauxbrothers.parkshark.parkingLot.domain.Category;
import com.bordeauxbrothers.parkshark.person.api.dtos.AddressDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Information required to create a parking lot.")
public record CreateParkingLotDto(

        @Schema(description = "Name of the parking lot.", example = "Shark Central Underground")
        @NotBlank(message = "parking lot name should not be blank")
        String name,

        @Schema(description = "Type of building the parking lot is in.", example = "UNDERGROUND_BUILDING")
        @NotNull(message = "parking lot should have a category")
        Category category,

        @Schema(description = "Maximum number of cars that can be parked at the same time.", example = "250")
        @NotNull(message = "parking lot should have a max capacity")
        @Positive(message = "max capacity should be greater than 0")
        Integer maxCapacity,

        @Schema(description = "Price per hour in euro for parking a car.", example = "2.50")
        @NotNull(message = "parking lot should have a price per hour")
        @Positive(message = "price per hour should be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "price per hour should have at most 2 decimals")
        BigDecimal pricePerHour,

        @Schema(description = "Address of the parking lot.")
        @NotNull(message = "parking lot should have an address")
        @Valid
        AddressDTO address,

        @Schema(description = "ID of an existing member who is the contact person.", example = "7")
        @NotNull(message = "parking lot should have a contact person")
        @Positive(message = "Contact person ID must be greater than zero")
        Integer contactPersonId,

        @Schema(description = "ID of an existing division or subdivision.", example = "1")
        @NotNull(message = "parking lot should belong to a division")
        @Positive(message = "Division ID must be greater than zero")
        Integer divisionId
) {
}
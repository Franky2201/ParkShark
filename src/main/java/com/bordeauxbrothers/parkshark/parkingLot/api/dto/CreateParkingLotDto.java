package com.bordeauxbrothers.parkshark.parkingLot.api.dto;

import com.bordeauxbrothers.parkshark.parkingLot.domain.Category;
import com.bordeauxbrothers.parkshark.person.service.dtos.AddressDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateParkingLotDto(
        @NotBlank(message = "parking lot name should not be blank") String name,
        @NotNull(message = "parking lot should have a category") Category category,
        @NotNull(message = "parking lot should have a max capacity")
        @Positive(message = "max capacity should be greater than 0") Integer maxCapacity,
        @NotNull(message = "parking lot should have a price per hour")
        @Positive(message = "price per hour should be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "price per hour should have at most 2 decimals") BigDecimal pricePerHour,
        @NotNull(message = "parking lot should have an address")
        @Valid AddressDTO address,
        @NotNull(message = "parking lot should have a contact person") Integer contactPersonId,
        @NotNull(message = "parking lot should belong to a division") Integer divisionId
) {
}

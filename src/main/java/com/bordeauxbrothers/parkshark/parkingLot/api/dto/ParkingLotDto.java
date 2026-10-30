package com.bordeauxbrothers.parkshark.parkingLot.api.dto;

import com.bordeauxbrothers.parkshark.parkingLot.domain.Category;
import com.bordeauxbrothers.parkshark.person.service.dtos.AddressDTO;

import java.math.BigDecimal;

public record ParkingLotDto(
        Integer id,
        String name,
        Category category,
        Integer maxCapacity,
        BigDecimal pricePerHour,
        Integer contactPersonId,
        Integer divisionId,
        AddressDTO address
) {
}

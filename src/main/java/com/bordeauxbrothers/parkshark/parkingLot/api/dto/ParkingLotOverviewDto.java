package com.bordeauxbrothers.parkshark.parkingLot.api.dto;

public record ParkingLotOverviewDto(
        int id,
        String name,
        int capacity,
        String contactPersonEmail,
        String contactPersonTelephone
) {
}

package com.bordeauxbrothers.parkshark.allocation.exception;

import com.bordeauxbrothers.parkshark.common.exception.ResourceNotFoundException;

public class ParkingLotNotFoundException extends ResourceNotFoundException {
    public ParkingLotNotFoundException(int id) {
        super("Parking lot with id %d not found".formatted(id));
    }
}
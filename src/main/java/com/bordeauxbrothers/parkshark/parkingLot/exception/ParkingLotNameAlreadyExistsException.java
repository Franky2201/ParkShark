package com.bordeauxbrothers.parkshark.parkingLot.exception;

import com.bordeauxbrothers.parkshark.common.exception.ConflictException;

public class ParkingLotNameAlreadyExistsException extends ConflictException {
    public ParkingLotNameAlreadyExistsException(String name) {
        super("A parking lot with name '%s' already exists".formatted(name));
    }
}

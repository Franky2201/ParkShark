package com.bordeauxbrothers.parkshark.parkingLot.exception;

import com.bordeauxbrothers.parkshark.common.exception.BadRequestException;

public class InvalidContactPersonException extends BadRequestException {
    public InvalidContactPersonException(int id, String reason) {
        super("Contact person with id %d is invalid: %s".formatted(id, reason));
    }
}

package com.bordeauxbrothers.parkshark.person.exception;

import com.bordeauxbrothers.parkshark.common.exception.ParkSharkException;

public class EmailAlreadyInUseException extends ParkSharkException {
    public EmailAlreadyInUseException(String message) {
        super(message);
    }
}

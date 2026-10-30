package com.bordeauxbrothers.parkshark.common.security.exception;

import com.bordeauxbrothers.parkshark.common.exception.ParkSharkException;

public class ForbiddenException extends ParkSharkException {
    public ForbiddenException(String message) {
        super(message);
    }
}

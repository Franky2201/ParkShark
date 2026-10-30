package com.bordeauxbrothers.parkshark.common.security.exception;

import com.bordeauxbrothers.parkshark.common.exception.ParkSharkException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class UnauthorizedException extends ParkSharkException {
    public UnauthorizedException() {
        super("Invalid credentials");
    }
}

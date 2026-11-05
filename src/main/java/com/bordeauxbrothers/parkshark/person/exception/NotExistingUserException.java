package com.bordeauxbrothers.parkshark.person.exception;

import com.bordeauxbrothers.parkshark.common.exception.ParkSharkException;
public class NotExistingUserException extends ParkSharkException {
    public NotExistingUserException(String message) {
        super(message);
    }
}

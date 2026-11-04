package com.bordeauxbrothers.parkshark.person.exception;

import com.bordeauxbrothers.parkshark.common.exception.ParkSharkException;

public class InvalidMembershipException extends ParkSharkException {
    public InvalidMembershipException(String message) {
        super(message);
    }
}

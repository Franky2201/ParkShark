package com.bordeauxbrothers.parkshark.person.exception;

import com.bordeauxbrothers.parkshark.common.exception.ParkSharkException;

public class UnchangedMembershipException extends ParkSharkException {
    public UnchangedMembershipException(String message) {
        super(message);
    }
}

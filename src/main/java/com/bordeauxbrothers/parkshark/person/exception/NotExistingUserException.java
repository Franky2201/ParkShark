package com.bordeauxbrothers.parkshark.person.exception;

import com.bordeauxbrothers.parkshark.common.exception.ResourceNotFoundException;

public class NotExistingUserException extends ResourceNotFoundException {
    public NotExistingUserException(int id) {
        super("Member with id %d not found".formatted(id));
    }
}

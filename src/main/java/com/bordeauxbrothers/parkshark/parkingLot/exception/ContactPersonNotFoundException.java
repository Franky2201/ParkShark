package com.bordeauxbrothers.parkshark.parkingLot.exception;

import com.bordeauxbrothers.parkshark.common.exception.ResourceNotFoundException;

public class ContactPersonNotFoundException extends ResourceNotFoundException {
    public ContactPersonNotFoundException(int id) {
        super("Contact person with id %d not found".formatted(id));
    }
}

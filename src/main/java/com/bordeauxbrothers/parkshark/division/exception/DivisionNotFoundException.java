package com.bordeauxbrothers.parkshark.division.exception;

import com.bordeauxbrothers.parkshark.common.exception.ResourceNotFoundException;

public class DivisionNotFoundException extends ResourceNotFoundException {
    public DivisionNotFoundException(int id) {
        super("Division with id %d not found".formatted(id));
    }
}

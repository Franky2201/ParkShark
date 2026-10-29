package com.bordeauxbrothers.parkshark.common.exception;

public abstract class ParkSharkException extends RuntimeException {
    protected ParkSharkException(String message) { super(message); }
}

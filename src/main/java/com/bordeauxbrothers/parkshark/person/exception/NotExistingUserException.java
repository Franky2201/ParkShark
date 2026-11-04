package com.bordeauxbrothers.parkshark.person.exception;

import com.bordeauxbrothers.parkshark.common.exception.ParkSharkException;
import org.slf4j.Logger;

public class NotExistingUserException extends ParkSharkException {
    private Logger logger = org.slf4j.LoggerFactory.getLogger(NotExistingUserException.class);
    public NotExistingUserException(String message) {
        super(message);
        logger.info("Member with provided id not found!");
    }
}

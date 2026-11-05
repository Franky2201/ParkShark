package com.bordeauxbrothers.parkshark.person.service.customvalidators;

import com.bordeauxbrothers.parkshark.person.api.dtos.AddMemberDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AtLeastOnePhoneValidator
        implements ConstraintValidator<AtLeastOnePhone, AddMemberDTO> {

    Logger logger = LoggerFactory.getLogger(AtLeastOnePhoneValidator.class);

    @Override
    public boolean isValid(
            AddMemberDTO dto,
            ConstraintValidatorContext context) {

        if (dto == null) {
            return true;
        }

        logger.debug("Checking if at least one phone number is present.");
        return isNotBlank(dto.phoneNumber())
                || isNotBlank(dto.mobileNumber());
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}

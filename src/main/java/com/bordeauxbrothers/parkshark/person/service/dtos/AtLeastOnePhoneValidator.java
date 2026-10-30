package com.bordeauxbrothers.parkshark.person.service.dtos;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AtLeastOnePhoneValidator
        implements ConstraintValidator<AtLeastOnePhone, AddMemberDTO> {

    @Override
    public boolean isValid(
            AddMemberDTO dto,
            ConstraintValidatorContext context) {

        if (dto == null) {
            return true;
        }

        return isNotBlank(dto.getPhoneNumber())
                || isNotBlank(dto.getMobileNumber());
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}

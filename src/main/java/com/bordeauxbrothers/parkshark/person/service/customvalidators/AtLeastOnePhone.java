package com.bordeauxbrothers.parkshark.person.service.customvalidators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AtLeastOnePhoneValidator.class)
public @interface AtLeastOnePhone {
    String message() default "Either mobile or phone number is required";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
package com.fisa.validationapi.infrastructure.adapters.input.rest.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.Period;

public class AgeLimitValidator implements ConstraintValidator<AgeLimit, LocalDate> {

    private int maxAge;

    @Override
    public void initialize(AgeLimit constraintAnnotation) {
        this.maxAge = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(LocalDate dateOfBirth, ConstraintValidatorContext context) {
        // Calcular la edad
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();

        // Validar que sea menor o igual al máximo
        return age <= maxAge;
    }
}
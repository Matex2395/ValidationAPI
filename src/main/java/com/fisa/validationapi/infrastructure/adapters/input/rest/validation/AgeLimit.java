package com.fisa.validationapi.infrastructure.adapters.input.rest.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AgeLimitValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AgeLimit {
    String message() default "La edad no puede exceder los {max} años";
    int max() default 120; // Valor por defecto

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
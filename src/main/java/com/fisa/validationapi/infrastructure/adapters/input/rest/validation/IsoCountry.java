package com.fisa.validationapi.infrastructure.adapters.input.rest.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = IsoCountryValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsoCountry {
    String message() default "Código de País ISO Inválido. Por favor use un código ISO 3166-1 alpha-2 válido (e.g., US, EC, CL).";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
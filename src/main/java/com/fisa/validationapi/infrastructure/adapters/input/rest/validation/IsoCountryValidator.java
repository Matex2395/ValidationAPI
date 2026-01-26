package com.fisa.validationapi.infrastructure.adapters.input.rest.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class IsoCountryValidator implements ConstraintValidator<IsoCountry, String> {

    // Cargar los códigos en un Set estático para acceso rápido (O(1))
    private static final Set<String> ISO_COUNTRIES = Arrays.stream(Locale.getISOCountries())
            .collect(Collectors.toSet());

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Si es nulo, dejar que @NotNull o @NotBlank se encarguen
        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        // Validar si el código (en mayúsculas) existe en la lista oficial
        return ISO_COUNTRIES.contains(value.toUpperCase());
    }
}
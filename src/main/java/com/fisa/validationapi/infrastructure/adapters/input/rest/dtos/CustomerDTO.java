package com.fisa.validationapi.infrastructure.adapters.input.rest.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fisa.validationapi.domain.models.enums.GenderEnum;
import com.fisa.validationapi.domain.models.enums.IdentityTypeEnum;
import com.fisa.validationapi.infrastructure.adapters.input.rest.validation.AgeLimit;
import com.fisa.validationapi.infrastructure.adapters.input.rest.validation.IsoCountry;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CustomerDTO {

    // --- IDENTIFICACIÓN ---

    @NotNull(message = "El tipo de identificación es obligatorio")
    private IdentityTypeEnum identityType; // Jackson valida que sea RUT, CEDULA o PASAPORTE

    @NotBlank(message = "El número de identificación es obligatorio")
    // Solo números y guiones (ej: 890-432-5787)
    @Pattern(regexp = "^[0-9\\-]+$", message = "El número de identificación solo permite dígitos y guiones")
    private String identityNumber;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @Future(message = "La fecha de expiración de la Identificación debe estar en el futuro")
    private LocalDate identityExpiryDate;

    // --- DATOS PERSONALES ---

    @NotBlank(message = "El nombre completo es Obligatorio")
    // Letras, espacios y acentos (\u00C0-\u017F cubre tildes, ñ, diéresis).
    @Pattern(regexp = "^[a-zA-Z\\u00C0-\\u017F\\s]+$", message = "El nombre completo solo permite letras y espacios")
    private String fullLegalName;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    @AgeLimit(max = 120, message = "El cliente no puede ser mayor de 120 años")
    private LocalDate dateOfBirth;

    private GenderEnum genderCode; // Enum M o F

    @NotBlank(message = "El código de nacionalidad es obligatorio")
    @Size(min = 2, max = 2, message = "El código de nacionalidad debe estar compuesto exactamente por 2 caracteres")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "El código de nacionalidad solo permite letras")
    @IsoCountry(message = "El código de país no corresponde a un país válido (ISO 3166-1 alpha-2)")
    private String nationalityCode;

    // --- DEMOGRAFÍA ---

    @NotBlank(message = "El nivel educativo es obligatorio")
    @Pattern(regexp = "^[a-zA-Z\\u00C0-\\u017F\\s]+$", message = "El nivel de educación solo permite letras")
    private String educationLevel;

    @NotBlank(message = "La ocupación es obligatoria")
    @Pattern(regexp = "^[a-zA-Z\\u00C0-\\u017F\\s]+$", message = "La ocupación solo permite letras")
    private String occupationCode;

    @NotBlank(message = "El estado civil es obligatorio")
    @Pattern(regexp = "^[a-zA-Z\\u00C0-\\u017F\\s]+$", message = "El estado civil solo permite letras")
    private String maritalStatusCode;

    // --- CONTACTO ---

    @Email(message = "Formato de correo electrónico inválido")
    private String emailAddress;

    @Pattern(regexp = "^[0-9\\+\\(\\)\\s]+$", message = "El número telefónico solo permite dígitos, +, (, ) y espacios")
    private String phoneNumber;

    // --- UBICACIÓN ---

    private String addressLine;

    private String postCode;

    @NotBlank(message = "El nombre de la ciudad es obligatorio")
    @Pattern(regexp = "^[a-zA-Z\\u00C0-\\u017F\\s]+$", message = "El nombre de la ciudad solo permite letras y espacios")
    private String townName;

    @NotBlank(message = "El código de país es obligatorio")
    @Size(min = 2, max = 2, message = "El código de país debe estar compuesto exactamente por 2 caracteres")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "El código de país solo permite letras")
    @IsoCountry(message = "El código de nacionalidad no corresponde a un país válido (ISO 3166-1 alpha-2)")
    private String countryCode;
}
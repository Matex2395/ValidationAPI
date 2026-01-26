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
public class CustomerReferenceDTO {

    @NotNull(message = "Identity Type is mandatory")
    private IdentityTypeEnum identityType;

    @NotBlank(message = "Identity Number cannot be blank")
    @Pattern(regexp = "^[0-9\\-]+$", message = "Identity Number allows only digits and hyphens")
    private String identityNumber;

    @NotBlank(message = "Full Legal Name is mandatory")
    @Pattern(regexp = "^[a-zA-Z\\u00C0-\\u017F\\s]+$", message = "Full Legal Name allows only letters and spaces")
    private String fullLegalName;

    @NotNull(message = "Date of Birth is mandatory")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Past(message = "Date of Birth must be in the past")
    @AgeLimit(max = 120, message = "Customer cannot be older than 120 years")
    private LocalDate dateOfBirth;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Future(message = "Identity expiry date must be in the future")
    private LocalDate identityExpiryDate;

    @NotBlank(message = "Nationality Code is mandatory")
    @Size(min = 2, max = 2, message = "Nationality Code must be exactly 2 characters")
    @IsoCountry(message = "Nationality Code must be a valid ISO Alpha-2 code (e.g., CL, EC)")
    private String nationalityCode;

    @NotBlank(message = "Country Code is mandatory")
    @Size(min = 2, max = 2, message = "Country Code must be exactly 2 characters")
    @IsoCountry(message = "Country Code must be a valid ISO Alpha-2 code (e.g., CL, EC)")
    private String countryCode;

    @NotBlank(message = "Town Name is mandatory")
    @Pattern(regexp = "^[a-zA-Z\\u00C0-\\u017F\\s]+$", message = "Town Name allows only letters")
    private String townName;

    // --- CONTACTO ---
    @Email(message = "Invalid Email Address format")
    private String emailAddress;

    @Pattern(regexp = "^[0-9\\+\\(\\)\\s]+$", message = "Phone Number allows only digits, +, (, ) and spaces")
    private String phoneNumber;

    private GenderEnum genderCode;
    private String addressLine;
    private String postCode;
}
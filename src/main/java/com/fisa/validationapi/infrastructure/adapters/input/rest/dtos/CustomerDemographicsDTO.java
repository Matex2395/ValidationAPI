package com.fisa.validationapi.infrastructure.adapters.input.rest.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CustomerDemographicsDTO {

    @NotBlank(message = "Education Level is mandatory")
    @Pattern(regexp = "^[a-zA-Z\\u00C0-\\u017F\\s]+$", message = "Education Level allows only letters")
    private String educationLevel;

    @NotBlank(message = "Occupation Code is mandatory")
    @Pattern(regexp = "^[a-zA-Z\\u00C0-\\u017F\\s]+$", message = "Occupation Code allows only letters")
    private String occupationCode;

    @NotBlank(message = "Marital Status is mandatory")
    @Pattern(regexp = "^[a-zA-Z\\u00C0-\\u017F\\s]+$", message = "Marital Status allows only letters")
    private String maritalStatusCode;
}
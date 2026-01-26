package com.fisa.validationapi.infrastructure.adapters.input.rest.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CustomerRequestDTO {

    @Valid // ¡Importante para validar el objeto hijo!
    @NotNull(message = "Reference data is mandatory")
    private CustomerReferenceDTO referenceData;

    @Valid // ¡Importante para validar el objeto hijo!
    @NotNull(message = "Demographics data is mandatory")
    private CustomerDemographicsDTO demographics;
}
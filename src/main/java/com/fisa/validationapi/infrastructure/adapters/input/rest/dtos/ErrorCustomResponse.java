package com.fisa.validationapi.infrastructure.adapters.input.rest.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorCustomResponse {
    private String origin;    // Microservicio que falló
    private String errorType; // Tipo de error
    private String message;   // Detalle técnico
    private String path;      // Endpoint que se llamó
}
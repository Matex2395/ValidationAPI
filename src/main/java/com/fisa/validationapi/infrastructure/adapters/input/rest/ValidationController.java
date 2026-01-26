package com.fisa.validationapi.infrastructure.adapters.input.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisa.validationapi.domain.models.IdempotencyRecord;
import com.fisa.validationapi.domain.ports.in.ValidateTransactionUseCase;
import com.fisa.validationapi.infrastructure.adapters.input.rest.dtos.CustomerRequestDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated; // Necesario para que funcionen las validaciones de Headers
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/compliance")
@RequiredArgsConstructor
@Validated
public class ValidationController {

    private final ValidateTransactionUseCase validateTransactionUseCase;
    private final ObjectMapper objectMapper; // Inyectamos Jackson para serializar

    @PostMapping(value = "/validate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> validateTransaction(
            @RequestHeader(value = "x-idempotency-key")
            @NotBlank(message = "x-idempotency-key no puede estar en blanco")
            String idempotencyKey,

            @RequestHeader(value = "x-fapi-interaction-id")
            @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "x-fapi-interaction-id debe ser un UUID válido")
            String interactionId,

            @RequestHeader(value = "Consent-ID")
            @NotBlank(message = "Consent-ID no puede estar en blanco")
            String consentId,

            // CAMBIO: Solo se recibe el Objeto. Spring valida (@Valid) antes de entrar al código.
            @Valid @RequestBody CustomerRequestDTO customerRequestDTO
    ) {
        MDC.put("interactionId", interactionId);
        MDC.put("idempotencyKey", idempotencyKey);

        try {
            // 1. Convertir DTO a JSON String (Serialización manual)
            String jsonPayload = objectMapper.writeValueAsString(customerRequestDTO);

            // 2. Llamada al Negocio (Caso de Uso)
            IdempotencyRecord result = validateTransactionUseCase.validateAndProcess(idempotencyKey, jsonPayload, interactionId);

            // 3. Mapeo de Respuesta
            return switch (result.getStatus()) {
                case COMPLETED -> ResponseEntity.status(result.getHttpStatusCode())
                        .header("X-Idempotency-Hit", "true")
                        .body(result.getResponseBody());

                case FAILED -> ResponseEntity.status(result.getHttpStatusCode())
                        .body(result.getResponseBody());

                case PROCESSING -> ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("{\"error\": \"La transacción está siendo procesada. Por favor espere.\"}");
            };

        } catch (JsonProcessingException e) {
            // Error técnico si falla la conversión de Objeto a String (Raro, pero posible)
            log.error("Error serializando DTO a JSON", e);
            return ResponseEntity.internalServerError().body("{\"error\": \"Error interno procesando la solicitud JSON\"}");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        } finally {
            MDC.clear();
        }
    }
}
package com.fisa.validationapi.infrastructure.adapters.input.rest;

import com.fisa.validationapi.domain.models.IdempotencyRecord;
import com.fisa.validationapi.domain.models.enums.IdempotencyStatus;
import com.fisa.validationapi.domain.ports.in.ValidateTransactionUseCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/compliance")
@RequiredArgsConstructor
public class ValidationController {

    // Inyectar el Caso de Uso (Input Port)
    private final ValidateTransactionUseCase validateTransactionUseCase;

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

            @RequestBody String jsonPayload
    ) {
        MDC.put("interactionId", interactionId);
        MDC.put("idempotencyKey", idempotencyKey);

        try {
            // Validaciones HTTP (Headers)
            validateHeaders(interactionId, consentId);

            // Llamada al Negocio (Caso de Uso)
            IdempotencyRecord result = validateTransactionUseCase.validateAndProcess(idempotencyKey, jsonPayload, interactionId);

            // Mapeo de Respuesta (Domain -> HTTP)
            return switch (result.getStatus()) {
                case COMPLETED ->
                    // Agregar un header-custom para avisar al cliente que es una respuesta cacheada.
                    ResponseEntity.status(result.getHttpStatusCode())
                            .header("X-Idempotency-Hit", "true")
                            .body(result.getResponseBody());
                case FAILED -> ResponseEntity.status(result.getHttpStatusCode()).body(result.getResponseBody());

                // Si está PROCESSING, se devuelve el código HTTP 409 CONFLICT
                case PROCESSING -> ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("{\"error\": \"La transacción está siendo procesada. Por favor espere.\"}");
            };

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        } finally {
            MDC.clear();
        }
    }

    private void validateHeaders(String interactionId, String consentId) {
        try {
            UUID.fromString(interactionId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Header 'x-fapi-interaction-id' must be a valid UUID");
        }
        if (consentId == null || consentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Header 'Consent-ID' is mandatory");
        }
    }
}
package com.fisa.validationapi.infrastructure.adapters.input.rest;

import com.fisa.validationapi.domain.models.IdempotencyRecord;
import com.fisa.validationapi.domain.models.enums.IdempotencyStatus;
import com.fisa.validationapi.domain.ports.in.ValidateTransactionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/validate")
    public ResponseEntity<?> validateTransaction(
            @RequestHeader(value = "x-idempotency-key") String idempotencyKey,
            @RequestHeader(value = "x-fapi-interaction-id") String interactionId,
            @RequestHeader(value = "Consent-ID") String consentId,
            @RequestBody String jsonPayload
    ) {
        MDC.put("interactionId", interactionId);
        MDC.put("idempotencyKey", idempotencyKey);

        try {
            // Validaciones HTTP (Headers)
            validateHeaders(interactionId, consentId);

            // Llamada al Negocio (Caso de Uso)
            IdempotencyRecord result = validateTransactionUseCase.validateAndProcess(idempotencyKey, jsonPayload);

            // Mapeo de Respuesta (Domain -> HTTP)
            return switch (result.getStatus()) {
                case COMPLETED, FAILED -> ResponseEntity.status(result.getHttpStatusCode()).body(result.getResponseBody());
                case PROCESSING -> ResponseEntity.status(HttpStatus.CONFLICT).body("{\"error\": \"Request is currently being processed\"}");
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
package com.fisa.validationapi.infrastructure.adapters.input.rest;

import com.fisa.validationapi.application.services.IdempotencyService;
import com.fisa.validationapi.domain.models.IdempotencyRecord;
import com.fisa.validationapi.domain.models.enums.IdempotencyStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/compliance")
@RequiredArgsConstructor
public class ValidationController {

    private final IdempotencyService idempotencyService;
    // private final Iso20022ValidatorService isoValidatorService;

    @PostMapping("/validate")
    public ResponseEntity<?> validateTransaction(
            @RequestHeader(value = "x-idempotency-key") String idempotencyKey,
            @RequestHeader(value = "x-fapi-interaction-id") String interactionId,
            @RequestHeader(value = "Consent-ID") String consentId,
            @RequestBody String jsonPayload // Se recibe como String para validar estructura cruda
    ) {
        // Trazabilidad (MDC para logs en ELK)
        MDC.put("interactionId", interactionId);
        MDC.put("idempotencyKey", idempotencyKey);

        log.info("VALIDATION-API: Recibida petición. Consent-ID: {}", consentId);

        try {
            // Validaciones básicas de Headers (Compliance Open Banking)
            validateHeaders(interactionId, consentId);

            // Comprobar de Idempotencia (Redis)
            Optional<IdempotencyRecord> existingRecord = idempotencyService.checkAndLock(idempotencyKey);

            if (existingRecord.isPresent()) {
                IdempotencyRecord record = existingRecord.get();

                // CASO A: Ya se procesó exitosamente antes
                if (record.getStatus() == IdempotencyStatus.COMPLETED) {
                    log.info("Idempotencia: Retornando respuesta guardada.");
                    return ResponseEntity
                            .status(record.getHttpStatusCode())
                            .body(record.getResponseBody());
                }

                // CASO B: Se está procesando ahora mismo (Condición de carrera)
                if (record.getStatus() == IdempotencyStatus.PROCESSING) {
                    log.warn("Idempotencia: Conflicto. La petición ya se está procesando.");
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body("{\"error\": \"Request is currently being processed\"}");
                }

                // CASO C: Falló antes, permitimos reintentar (sigue el flujo hacia abajo)
                log.info("Idempotencia: Reintento de una petición fallida previamente.");
            }

            // Validación de Negocio (ISO 20022 con Prowide)
            log.info("Iniciando validación ISO 20022...");

            // --- TODO: Descomentar cuando se cree el IsoValidatorService ---
            // isoValidatorService.validateJsonStructure(jsonPayload);
            // ---------------------------------------------------------------

            // Si todo sale bien se guarda en Redis el proceso como exitoso
            String successResponse = "{\"status\": \"COMPLIANT\", \"message\": \"ISO 20022 Validation Passed\"}";
            idempotencyService.saveSuccess(idempotencyKey, 200, successResponse);

            return ResponseEntity.ok(successResponse);

        } catch (IllegalArgumentException e) {
            // Error de validación (Headers o ISO mal formado)
            log.error("Validation Failed: {}", e.getMessage());
            // Se guarda el fallo en Redis para no bloquear reintentos futuros
            idempotencyService.saveFailure(idempotencyKey, e.getMessage());
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");

        } catch (Exception e) {
            // Error técnico inesperado
            log.error("Internal Server Error: {}", e.getMessage(), e);
            idempotencyService.saveFailure(idempotencyKey, "Internal Error");
            return ResponseEntity.internalServerError().body("{\"error\": \"Internal Server Error\"}");

        } finally {
            MDC.clear(); // Limpieza obligatoria del contexto
        }
    }

    private void validateHeaders(String interactionId, String consentId) {
        // Validar formato UUID del interaction-id
        try {
            UUID.fromString(interactionId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Header 'x-fapi-interaction-id' must be a valid UUID");
        }

        if (consentId == null || consentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Header 'Consent-ID' is mandatory for Open Banking compliance");
        }
    }
}
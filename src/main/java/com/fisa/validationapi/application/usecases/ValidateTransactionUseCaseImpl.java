package com.fisa.validationapi.application.usecases;

import com.fisa.validationapi.application.services.IdempotencyService;
import com.fisa.validationapi.application.services.Iso20022ValidatorService;
import com.fisa.validationapi.domain.models.IdempotencyRecord;
import com.fisa.validationapi.domain.models.enums.IdempotencyStatus;
import com.fisa.validationapi.domain.ports.in.ValidateTransactionUseCase;
import com.fisa.validationapi.infrastructure.adapters.output.feign.PartyServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class ValidateTransactionUseCaseImpl implements ValidateTransactionUseCase {

    private final IdempotencyService idempotencyService;
    private final Iso20022ValidatorService isoValidatorService;
    private final PartyServiceClient partyServiceClient;

    @Override
    public IdempotencyRecord validateAndProcess(String idempotencyKey, String jsonPayload) {

        // IDEMPOTENCIA: Verificar si ya existe
        Optional<IdempotencyRecord> existing = idempotencyService.checkAndLock(idempotencyKey);
        if (existing.isPresent()) {
            // Si ya existe (Processing o Completed), se retorna tal cual.
            // El Controller decidirá cuál código HTTP enviar.
            return existing.get();
        }

        // Si se llega aquí, es una petición NUEVA y ya se puso el estado 'PROCESSING' para validar idempotencia.
        try {
            // Validar Tipos de datos ISO 20022
            isoValidatorService.validateJsonStructure(jsonPayload);
            log.info("Validación ISO 20022 exitosa para idempotencyKey {}", idempotencyKey);

            // Llamada a Party Service Operation (OPEN FEIGN)
            ResponseEntity<Object> response = partyServiceClient.createParty(jsonPayload);

            // Obtenemos el body de la respuesta del Party Service
            String responseBody = response.getBody() != null ? response.getBody().toString() : "Success";
            int statusCode = response.getStatusCode().value();

            // ÉXITO: Actualizar Redis
            idempotencyService.saveSuccess(idempotencyKey, statusCode, responseBody);

            return IdempotencyRecord.builder()
                    .key(idempotencyKey)
                    .status(IdempotencyStatus.COMPLETED)
                    .httpStatusCode(statusCode)
                    .responseBody(responseBody)
                    .build();

        } catch (IllegalArgumentException e) {
            // Error de Validación (400)
            log.warn("Validation Error: {}", e.getMessage());
            idempotencyService.saveFailure(idempotencyKey, e.getMessage());

            return IdempotencyRecord.builder()
                    .status(IdempotencyStatus.FAILED)
                    .httpStatusCode(400)
                    .responseBody("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();

        } catch (Exception e) {
            // Error Técnico o Party Service caído (500)
            log.error("System Error processing transaction: {}", e.getMessage());

            // Importante: Si el Party Service falla, se guarda el error
            String errorMsg = "Internal Error or Party Service Unavailable";
            idempotencyService.saveFailure(idempotencyKey, errorMsg);

            return IdempotencyRecord.builder()
                    .status(IdempotencyStatus.FAILED)
                    .httpStatusCode(500)
                    .responseBody("{\"error\": \"" + errorMsg + "\"}")
                    .build();
        }
    }
}
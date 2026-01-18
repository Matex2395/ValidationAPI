package com.fisa.validationapi.domain.ports.in;

import com.fisa.validationapi.domain.models.IdempotencyRecord;

public interface ValidateTransactionUseCase {

    /**
     * Ejecuta todo el flujo de validación:
     * 1. Chequeo de Idempotencia.
     * 2. Validación ISO 20022.
     * 3. Guardado de resultados.
     */
    IdempotencyRecord validateAndProcess(String idempotencyKey, String jsonPayload);
}
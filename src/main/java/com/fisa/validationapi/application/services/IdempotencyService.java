package com.fisa.validationapi.application.services;

import com.fisa.validationapi.domain.models.IdempotencyRecord;
import com.fisa.validationapi.domain.models.enums.IdempotencyStatus;
import com.fisa.validationapi.domain.ports.out.IdempotencyRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepositoryPort idempotencyRepository;

    /**
     * Intenta iniciar una transacción idempotente.
     * * @param key La llave de idempotencia (x-idempotency-key).
     * * @param interactionId El UUID de la petición actual (x-fapi-interaction-id).
     * @return Optional vacío si es una petición nueva (SE PUEDE PROCESAR).
     * Optional con IdempotencyRecord si ya existe (YA FUE PROCESADA o está en proceso).
     * @throws RuntimeException Si Redis está caído (Fail-Safe: no procesar si no se puede bloquear).
     */
    public Optional<IdempotencyRecord> checkAndLock(String key, String interactionId) {
        // Buscar si ya existe la llave
        Optional<IdempotencyRecord> existingRecord = idempotencyRepository.findByKey(key);

        if (existingRecord.isPresent()) {
            IdempotencyRecord record = existingRecord.get();
            // Devolver el registro original que contiene el resultado exitoso.
            log.info("Idempotency: Llave {} ya existe. Encontrada con estado {} y procesada originalmente por UUID: {}", key, record.getStatus(), record.getOriginalInteractionId());
            return existingRecord; // Devolver el registro existente indicando conflicto.
        }

        // Si no existe, se crea el bloqueo (Estado: PROCESSING)
        log.info("Idempotency: Llave {} nueva. Bloqueando bajo UUID {}", key, interactionId);

        IdempotencyRecord newRecord = IdempotencyRecord.builder()
                .key(key)
                .originalInteractionId(interactionId)
                .status(IdempotencyStatus.PROCESSING)
                .createdAt(LocalDateTime.now())
                .build();

        // Guardar en Redis.
        // Si Redis falla aquí, el Adapter lanzará excepción y el flujo se detendrá (Correcto).
        idempotencyRepository.save(newRecord);

        return Optional.empty(); // Indicar que se puede proseguir, pues no hay conflicto de idempotency-key.
    }

    /**
     * Finaliza la transacción exitosamente guardando la respuesta.
     */
    public void saveSuccess(String key, int httpStatus, String responseBody) {
        log.info("Idempotency: Actualizando llave {} a COMPLETED", key);

        Optional<IdempotencyRecord> existing = idempotencyRepository.findByKey(key);
        String originalUuid = existing.map(IdempotencyRecord::getOriginalInteractionId).orElse("UNKNOWN");

        IdempotencyRecord record = IdempotencyRecord.builder()
                .key(key)
                .originalInteractionId(originalUuid)
                .status(IdempotencyStatus.COMPLETED)
                .httpStatusCode(httpStatus)
                .responseBody(responseBody)
                .createdAt(LocalDateTime.now())
                .build();

        idempotencyRepository.save(record);
    }

    /**
     * Marca la transacción como fallida para permitir reintentos futuros (o bloquear según regla).
     */
    public void saveFailure(String key, String errorDetail) {
        log.warn("Idempotency: Marcando llave {} como FAILED", key);

        IdempotencyRecord record = IdempotencyRecord.builder()
                .key(key)
                .status(IdempotencyStatus.FAILED)
                .responseBody(errorDetail) // Se guarda el error para auditoría
                .createdAt(LocalDateTime.now())
                .build();

        idempotencyRepository.save(record);
    }
}
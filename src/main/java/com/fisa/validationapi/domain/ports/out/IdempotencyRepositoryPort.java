package com.fisa.validationapi.domain.ports.out;

import com.fisa.validationapi.domain.models.IdempotencyRecord;
import java.util.Optional;

public interface IdempotencyRepositoryPort {

    /**
     * Guarda o actualiza un registro de idempotencia.
     * La implementación (Adapter) se encargará de definir el TTL (tiempo de vida).
     * @param record El objeto de dominio a guardar.
     */
    void save(IdempotencyRecord record);

    /**
     * Busca una llave de idempotencia.
     * @param key La llave única (x-idempotency-key).
     * @return Un Optional que contiene el registro si existe, o vacío si no.
     */
    Optional<IdempotencyRecord> findByKey(String key);
}
package com.fisa.validationapi.infrastructure.adapters.output.redis;

import com.fisa.validationapi.domain.models.IdempotencyRecord;
import com.fisa.validationapi.domain.ports.out.IdempotencyRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisIdempotencyAdapter implements IdempotencyRepositoryPort {

    private final RedisTemplate<String, Object> redisTemplate;

    // Prefijo para organizar las llaves en Redis (ej: "idempotency:ABC-123")
    private static final String KEY_PREFIX = "idempotency:";

    // Tiempo de vida de la llave: 24 horas.
    private static final Duration TTL = Duration.ofHours(24);

    @Override
    public void save(IdempotencyRecord record) {
        String fullKey = KEY_PREFIX + record.getKey();

        try {
            // Guardamos el objeto y definimos su expiración en una sola operación atómica
            redisTemplate.opsForValue().set(fullKey, record, TTL);
            log.debug("Redis: Guardada llave {} con estado {}", fullKey, record.getStatus());
        } catch (Exception e) {
            log.error("Redis: Error al guardar llave {}: {}", fullKey, e.getMessage());
            throw e;
        }
    }

    @Override
    public Optional<IdempotencyRecord> findByKey(String key) {
        String fullKey = KEY_PREFIX + key;

        try {
            Object value = redisTemplate.opsForValue().get(fullKey);

            if (value instanceof IdempotencyRecord record) {
                return Optional.of(record);
            }
            return Optional.empty();

        } catch (Exception e) {
            log.error("Redis: Error al buscar llave {}: {}", fullKey, e.getMessage());
            return Optional.empty(); // Fallback seguro
        }
    }
}
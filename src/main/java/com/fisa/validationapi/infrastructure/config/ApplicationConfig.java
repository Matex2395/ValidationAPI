package com.fisa.validationapi.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisa.validationapi.application.services.IdempotencyService;
import com.fisa.validationapi.application.services.Iso20022ValidatorService;
import com.fisa.validationapi.application.usecases.ValidateTransactionUseCaseImpl;
import com.fisa.validationapi.domain.ports.in.ValidateTransactionUseCase;
import com.fisa.validationapi.domain.ports.out.IdempotencyRepositoryPort;
import com.fisa.validationapi.infrastructure.adapters.output.feign.PartyServiceClient;
import com.fisa.validationapi.infrastructure.adapters.output.redis.RedisIdempotencyAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class ApplicationConfig {

    // ---------------------------------------------------------
    // 1. INFRASTRUCTURE & HELPERS
    // ---------------------------------------------------------

    // Servicio de Dominio (ISO) - Necesita ObjectMapper de Jackson
    @Bean
    public Iso20022ValidatorService iso20022ValidatorService(ObjectMapper objectMapper) {
        return new Iso20022ValidatorService(objectMapper);
    }

    // ---------------------------------------------------------
    // 2. PORTS (ADAPTERS WIRING)
    // ---------------------------------------------------------

    // Adaptador de Salida (Redis)
    // Inyectar RedisTemplate que ya se configuró en RedisConfig
    @Bean
    public IdempotencyRepositoryPort idempotencyRepositoryPort(RedisTemplate<String, Object> redisTemplate) {
        return new RedisIdempotencyAdapter(redisTemplate);
    }

    // ---------------------------------------------------------
    // 3. DOMAIN SERVICES
    // ---------------------------------------------------------

    // Servicio de Dominio (Idempotencia)
    // Recibe el Puerto (Interfaz), no la implementación directa
    @Bean
    public IdempotencyService idempotencyService(IdempotencyRepositoryPort repositoryPort) {
        return new IdempotencyService(repositoryPort);
    }

    // ---------------------------------------------------------
    // 4. USE CASES (MAIN LOGIC)
    // ---------------------------------------------------------

    // Caso de Uso Principal (ValidateTransaction)
    // Se inyecta:
    // 1. Servicio de Idempotencia
    // 2. Servicio ISO
    // 3. (Futuro) Cliente Feign para llamar a Party Service
    @Bean
    public ValidateTransactionUseCase validateTransactionUseCase(
            IdempotencyService idempotencyService,
            Iso20022ValidatorService isoValidatorService,
            PartyServiceClient partyServiceClient
    ) {
        return new ValidateTransactionUseCaseImpl(
                idempotencyService,
                isoValidatorService,
                partyServiceClient
        );
    }
}
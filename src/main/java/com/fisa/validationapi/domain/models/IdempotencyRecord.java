package com.fisa.validationapi.domain.models;

import com.fisa.validationapi.domain.models.enums.IdempotencyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord implements Serializable {

    // Llave única (x-idempotency-key) enviada por el TPP
    private String key;

    // Estado actual (PROCESSING, COMPLETED, FAILED)
    private IdempotencyStatus status;

    // Código HTTP que responde el microservicio BIAN
    private Integer httpStatusCode;

    // Cuerpo de la respuesta en formato JSON (String)
    // Se guarda esto para devolver exactamente lo mismo al reintento
    private String responseBody;

    // Fecha de creación para auditoría
    private LocalDateTime createdAt;

    // ID de la petición realizada originalmente (x-fapi-interaction-id)
    private String originalInteractionId;
}
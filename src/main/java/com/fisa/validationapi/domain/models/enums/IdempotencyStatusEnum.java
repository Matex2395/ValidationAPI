package com.fisa.validationapi.domain.models.enums;

public enum IdempotencyStatus {
    /**
     * La petición se recibió y se está procesando en el Core/Party Service.
     * Si llega otra petición con la misma Key y este estado, se debe bloquear/esperar.
     */
    PROCESSING,

    /**
     * La operación terminó exitosamente.
     * Si llega otra petición, se devuelve la respuesta guardada inmediatamente.
     */
    COMPLETED,

    /**
     * Hubo un error.
     * Dependiendo de la regla de negocio, se podría permitir reintentar.
     */
    FAILED
}
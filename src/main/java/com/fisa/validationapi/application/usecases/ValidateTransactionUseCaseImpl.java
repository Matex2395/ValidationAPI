package com.fisa.validationapi.application.usecases;

import com.fasterxml.jackson.databind.ObjectMapper; // Necesario para leer errores del Mapper
import com.fisa.validationapi.application.services.IdempotencyService;
import com.fisa.validationapi.application.services.Iso20022ValidatorService;
import com.fisa.validationapi.domain.models.IdempotencyRecord;
import com.fisa.validationapi.domain.models.enums.IdempotencyStatus;
import com.fisa.validationapi.domain.ports.in.ValidateTransactionUseCase;
import com.fisa.validationapi.infrastructure.adapters.input.rest.dtos.ErrorCustomResponse;
import com.fisa.validationapi.infrastructure.adapters.output.feign.NotificationClient;
import com.fisa.validationapi.infrastructure.adapters.output.feign.PartyServiceClient;
import com.fisa.validationapi.infrastructure.adapters.output.feign.dtos.NotificationRequest;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class ValidateTransactionUseCaseImpl implements ValidateTransactionUseCase {

    private final IdempotencyService idempotencyService;
    private final Iso20022ValidatorService isoValidatorService;
    private final PartyServiceClient partyServiceClient;
    private final NotificationClient notificationClient;
    private final ObjectMapper objectMapper; // Permite leer el JSON de error del Mapper

    @Override
    public IdempotencyRecord validateAndProcess(String idempotencyKey, String jsonPayload, String interactionId) {

        // IDEMPOTENCIA
        Optional<IdempotencyRecord> existing = idempotencyService.checkAndLock(idempotencyKey, interactionId);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();

            // LÓGICA DE DETECCIÓN DE REINTENTO
            // Si el UUID guardado es diferente al que llega ahora, es un reintento del cliente.
            if (record.getOriginalInteractionId() != null && !record.getOriginalInteractionId().equals(interactionId)) {
                log.warn("RETRY DETECTED [Key: {}]: Petición actual UUID {} es un reintento de la original UUID {}. Se devuelve respuesta en caché.",
                        idempotencyKey, interactionId, record.getOriginalInteractionId());
            } else {
                log.info("IDEMPOTENCY HIT [Key: {}]: Replay exacto de UUID {}.", idempotencyKey, interactionId);
            }

            return record;
        }

        try {
            // VALIDACIÓN ISO
            isoValidatorService.validateJsonStructure(jsonPayload);
            log.info("Validación ISO 20022 exitosa para idempotencyKey {}", idempotencyKey);

            // LLAMADA A PARTY SERVICE
            ResponseEntity<String> response = partyServiceClient.createParty(jsonPayload);
            String responseBody = response.getBody() != null ? response.getBody() : "Success";
            int statusCode = response.getStatusCode().value();

            // ÉXITO: Actualizar Redis
            idempotencyService.saveSuccess(idempotencyKey, statusCode, responseBody);

            // ENVIAR CORREO DE ÉXITO ---
            sendSuccessEmail(idempotencyKey, responseBody);

            return IdempotencyRecord.builder()
                    .key(idempotencyKey)
                    .originalInteractionId(interactionId)
                    .status(IdempotencyStatus.COMPLETED)
                    .httpStatusCode(statusCode)
                    .responseBody(responseBody)
                    .build();

        } catch (IllegalArgumentException e) {
            // Error de Validación ISO (400)
            log.warn("Validation Error: {}", e.getMessage());
            // Guardar fallo en Redis
            idempotencyService.saveFailure(idempotencyKey, e.getMessage());

            // Enviar correo de alerta (Validación Fallida)
            sendErrorEmail("ValidationAPI (ISO Check)", "Error de formato: " + e.getMessage(), idempotencyKey);

            return IdempotencyRecord.builder()
                    .status(IdempotencyStatus.FAILED)
                    .httpStatusCode(400)
                    .responseBody("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();

        } catch (Exception e) {
            // Error Técnico / Caída de Servicios (500)
            log.error("System Error processing transaction: {}", e.getMessage());

            // ANÁLISIS DEL ERROR PARA EL CORREO
            // Se determina quién falló realmente (Party Service Operation o Mapper)
            String origin = "ValidationAPI";
            String detail = e.getMessage();

            if (e instanceof FeignException fe) {
                // Si es Feign, se intenta leer el JSON que envió el Party Service Operation/Mapper
                try {
                    if (fe.contentUTF8() != null) {
                        ErrorCustomResponse downstreamError = objectMapper.readValue(fe.contentUTF8(), ErrorCustomResponse.class);
                        origin = downstreamError.getOrigin();
                        detail = downstreamError.getMessage();
                    }
                } catch (Exception ignored) {
                    // Si falla el parsing, se queda el mensaje original
                }
            }
            // Guardar en Redis
            String errorMsg = "Fallo crítico en " + origin + ": " + detail;
            idempotencyService.saveFailure(idempotencyKey, errorMsg);
            // Enviar Correo de Error
            sendErrorEmail(origin, detail, idempotencyKey);

            return IdempotencyRecord.builder()
                    .status(IdempotencyStatus.FAILED)
                    .httpStatusCode(500)
                    .responseBody("{\"error\": \"" + errorMsg + "\"}")
                    .build();
        }
    }

    // --- MÉTODOS PRIVADOS AUXILIARES ---

    private void sendSuccessEmail(String trxId, String jsonResponse) {
        String emailBody;
        String subject = "Onboarding Exitoso - Nuevo Cliente";

        try {
            // Convertir el String JSON a un Árbol de Nodos para navegarlo
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            // Extraer los datos navegando por el JSON
            // Se usa .path() porque es seguro (si el campo no existe, no lanza error)
            String partyRefId = rootNode.path("partyRefId").asText("N/A");

            // root -> referenceData -> fullLegalName
            JsonNode refDataNode = rootNode.path("referenceData");
            String fullName = refDataNode.path("fullLegalName").asText("Cliente Desconocido");
            String idNumber = refDataNode.path("identityNumber").asText("N/A");

            // Construir el mensaje personalizado
            emailBody = String.format("""
            Hola,
            
            Se ha completado exitosamente el registro de un nuevo cliente.
            
            ------------------------------------------------
            Detalles del Cliente:
            ------------------------------------------------
            Nombre:           %s
            Identificación:   %s
            Referencia BIAN:  %s
            ------------------------------------------------
            
            ID de Transacción: %s
            """, fullName, idNumber, partyRefId, trxId);

        } catch (Exception e) {
            // Fallback: Si el JSON no tiene el formato esperado, se envía el mensaje genérico
            log.warn("No se pudo parsear la respuesta para el correo: {}", e.getMessage());
            emailBody = "El cliente ha sido creado correctamente.\nID Transacción: " + trxId;
        }

        // Enviar el correo
        try {
            NotificationRequest email = NotificationRequest.builder()
                    .recipient("cliente@banco.com")
                    .subject(subject)
                    .body(emailBody)
                    .originService("ValidationAPI")
                    .build();
            notificationClient.sendEmail(email);
            log.info("Correo de éxito detallado enviado.");
        } catch (Exception e) {
            log.warn("No se pudo enviar el correo de éxito: {}", e.getMessage());
        }
    }

    private void sendErrorEmail(String origin, String detail, String trxId) {
        try {
            NotificationRequest email = NotificationRequest.builder()
                    .recipient("admin@banco.com")
                    .subject("[ALERTA] Fallo en Transacción " + trxId)
                    .body("El flujo falló.\n\nCulpable: " + origin + "\nDetalle: " + detail)
                    .originService(origin)
                    .build();
            notificationClient.sendEmail(email);
        } catch (Exception e) {
            log.warn("No se pudo enviar el correo de alerta: {}", e.getMessage());
        }
    }
}
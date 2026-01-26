package com.fisa.validationapi.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisa.validationapi.infrastructure.adapters.input.rest.dtos.ErrorCustomResponse;
import com.fisa.validationapi.infrastructure.adapters.output.feign.NotificationClient;
import com.fisa.validationapi.infrastructure.adapters.output.feign.dtos.NotificationRequest;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.charset.StandardCharsets;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final NotificationClient notificationClient;
    private final ObjectMapper objectMapper; // Para convertir String JSON a Objeto Java

    // 1. Manejo de Errores de Microservicios Vecinos (Party / Mapper)
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorCustomResponse> handleFeignException(FeignException e, HttpServletRequest request) {
        log.error("Error recibido de un microservicio externo. Status: {}", e.status());

        String origin = "Unknown";
        String detail = e.getMessage();

        try {
            if (e.contentUTF8() != null) {
                // Convertir el cuerpo del error (String) al objeto DTO
                ErrorCustomResponse downstreamError = objectMapper.readValue(e.contentUTF8(), ErrorCustomResponse.class);

                origin = downstreamError.getOrigin();
                detail = downstreamError.getMessage();

                log.info("Culpable identificado: {}", origin);
            }
        } catch (Exception parseException) {
            log.warn("No se pudo parsear el error del microservicio: {}", parseException.getMessage());
        }

        // ENVIAR CORREO DE ALERTA
        sendErrorEmail(origin, "Fallo en flujo de Onboarding", detail);

        // Responder al TPP (Frontend)
        return buildResponse(HttpStatus.valueOf(e.status() > 0 ? e.status() : 500), "DependencyError", "Fallo en " + origin + ": " + detail, request);
    }

    // 2. Errores Internos (Validación ISO, Redis, NullPointer)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorCustomResponse> handleGeneralException(Exception e, HttpServletRequest request) {
        log.error("Error interno en ValidationAPI: {}", e.getMessage());

        // Enviar correo culpando a este microservicio
        sendErrorEmail("ValidationAPI", "Error Interno Crítico", e.getMessage());

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "InternalServerError", e.getMessage(), request);
    }

    // 3. Captura cuando falta el header por completo (Capa Spring)
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<String> handleMissingHeader(MissingRequestHeaderException ex) {
        String errorJson = String.format("{\"error\": \"Missing mandatory header: %s\"}", ex.getHeaderName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorJson);
    }

    // 4. Captura cuando el header está vacío o en blanco (Capa @NotBlank)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleValidationErrors(ConstraintViolationException ex) {
        String errorJson = String.format("{\"error\": \"Validation failed: %s\"}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorJson);
    }

    // --- Métodos Helper ---

    private void sendErrorEmail(String origin, String subject, String body) {
        try {
            NotificationRequest email = NotificationRequest.builder()
                    .recipient("admin@banco.com")
                    .subject("[ALERTA] " + subject)
                    .body("El servicio detectó un error.\n\nOrigen: " + origin + "\nDetalle: " + body)
                    .originService(origin)
                    .build();

            notificationClient.sendEmail(email);
        } catch (Exception ex) {
            log.error("¡Fallo al enviar el correo de alerta! El sistema de notificaciones no responde.", ex);
        }
    }

    private ResponseEntity<ErrorCustomResponse> buildResponse(HttpStatus status, String type, String message, HttpServletRequest request) {
        ErrorCustomResponse error = ErrorCustomResponse.builder()
                .origin("ValidationAPI")
                .errorType(type)
                .message(message)
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(error, status);
    }
}
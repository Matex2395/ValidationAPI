package com.fisa.validationapi.application.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class Iso20022ValidatorService {

    private final ObjectMapper objectMapper;

    // Patrones y Conjuntos ISO 20022 (Data Types)
    private static final Set<String> ISO_GENDERS = Set.of("M", "F", "U"); // Male, Female, Unknown
    private static final Pattern EMAIL_REGEX = Pattern.compile("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");

    // ISO 3166-1 alpha-3
    private static final Pattern ISO_COUNTRY_CODE_REGEX = Pattern.compile("^[A-Z]{3}$");

    /**
     * Valida que el DTO personalizado cumpla con los Data Types de ISO 20022.
     * @param jsonPayload El JSON recibido como String.
     */
    public void validateJsonStructure(String jsonPayload) {
        log.debug("Iniciando validación de reglas ISO 20022 sobre el payload...");

        try {
            // 1. Parsing Estructural (Jackson)
            JsonNode root = objectMapper.readTree(jsonPayload);

            // 2. Validar Bloque 'referenceData'
            JsonNode refData = root.path("referenceData");
            if (refData.isMissingNode()) {
                throw new IllegalArgumentException("ISO Validation: Missing 'referenceData' block");
            }

            // --- Validaciones de Tipos de Dato ISO ---

            // A. Validar Códigos de País (ISO 3166 - 3 letras)
            validateIsoCountry(refData.path("nationalityCode").asText(), "nationalityCode");
            validateIsoCountry(refData.path("countryCode").asText(), "countryCode");

            // B. Validar Fechas (ISO 8601 - YYYY-MM-DD)
            validateIsoDate(refData.path("dateOfBirth").asText(), "dateOfBirth");
            if (refData.hasNonNull("identityExpiryDate")) {
                validateIsoDate(refData.path("identityExpiryDate").asText(), "identityExpiryDate");
            }

            // C. Validar Género (ISO Codes)
            validateIsoGender(refData.path("genderCode").asText());

            // D. Validar Email (Restricciones de formato)
            validateEmail(refData.path("emailAddress").asText());

            // E. Validar Longitudes (Constraints típicos de ISO: Max35Text, Max70Text, Max140Text)
            validateLength(refData.path("fullLegalName").asText(), 140, "fullLegalName"); // ISO Name suele ser largo
            validateLength(refData.path("townName").asText(), 35, "townName"); // ISO TownName suele ser Max35Text
            validateLength(refData.path("addressLine").asText(), 70, "addressLine"); // ISO AddressLine suele ser Max70Text

            // 3. Validar Bloque 'demographics'
            JsonNode demographics = root.path("demographics");
            if (demographics.isMissingNode()) {
                throw new IllegalArgumentException("ISO Validation: Missing 'demographics' block");
            }

            // Validar campos obligatorios de demographics
            validateLength(demographics.path("occupationCode").asText(), 35, "occupationCode");
            validateLength(demographics.path("maritalStatusCode").asText(), 4, "maritalStatusCode"); // ISO suele usar códigos cortos (M, S, D) o listas externas

            log.info("Payload cumple con las reglas de tipos de datos ISO 20022.");

        } catch (IllegalArgumentException e) {
            throw e; // Re-lanzar errores de negocio
        } catch (Exception e) {
            log.error("Error parseando JSON", e);
            throw new IllegalArgumentException("ISO Validation: Invalid JSON Format - " + e.getMessage());
        }
    }

    // --- Métodos Helper Privados (Reglas ISO) ---

    private void validateIsoCountry(String code, String fieldName) {
        if (code == null || !ISO_COUNTRY_CODE_REGEX.matcher(code).matches()) {
            throw new IllegalArgumentException("ISO Validation: Field '" + fieldName + "' must be a valid ISO 3166-1 alpha-3 code (3 letters). Value: " + code);
        }
    }

    private void validateIsoDate(String dateStr, String fieldName) {
        if (dateStr == null || dateStr.isBlank()) return; // Si es opcional, se salta
        try {
            // ISO 20022 usa estrictamente YYYY-MM-DD (ISODate)
            LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("ISO Validation: Field '" + fieldName + "' must adhere to ISO 8601 format (YYYY-MM-DD). Value: " + dateStr);
        }
    }

    private void validateIsoGender(String gender) {
        if (gender == null || !ISO_GENDERS.contains(gender.toUpperCase())) {
            throw new IllegalArgumentException("ISO Validation: 'genderCode' must be one of [M, F, U] per ISO standards.");
        }
    }

    private void validateLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException("ISO Validation: Field '" + fieldName + "' exceeds ISO max length of " + maxLength + " characters.");
        }
    }

    private void validateEmail(String email) {
        if (email != null && !email.isBlank() && !EMAIL_REGEX.matcher(email).matches()) {
            throw new IllegalArgumentException("ISO Validation: Invalid email format.");
        }
    }
}
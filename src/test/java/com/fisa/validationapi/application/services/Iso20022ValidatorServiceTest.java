package com.fisa.validationapi.application.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Iso20022ValidatorServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private Iso20022ValidatorService service;

    // Utilidad local para construir JsonNode realista a partir de JSON
    private final ObjectMapper realMapper = new ObjectMapper();

    @Test
    void validate_happyPath_returnsValid() throws Exception {
        // Given: JSON mínimo válido
        String json = "{" +
                "\"referenceData\": {" +
                "\"fullLegalName\": \"John Doe\"," +
                "\"countryCode\": \"US\"," +
                "\"townName\": \"New York\"" +
                "}" +
                "}";
        JsonNode node = realMapper.readTree(json);
        when(objectMapper.readTree(json)).thenReturn(node);

        // When / Then: No lanza excepción
        assertThatCode(() -> service.validateJsonStructure(json))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_missingRequiredField_returnsInvalid() throws Exception {
        // Given: Falta referenceData
        String json = "{}";
        JsonNode node = realMapper.readTree(json);
        when(objectMapper.readTree(json)).thenReturn(node);

        // When / Then: Lanza IllegalArgumentException con mensaje específico
        assertThatThrownBy(() -> service.validateJsonStructure(json))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El bloque 'referenceData' es obligatorio.");
    }

    @Test
    void validate_invalidFormat_returnsInvalid() throws Exception {
        // Given: countryCode en minúsculas (formato inválido)
        String json = "{" +
                "\"referenceData\": {" +
                "\"fullLegalName\": \"Jane Smith\"," +
                "\"countryCode\": \"us\"," +
                "\"townName\": \"Miami\"" +
                "}" +
                "}";
        JsonNode node = realMapper.readTree(json);
        when(objectMapper.readTree(json)).thenReturn(node);

        // When / Then: Falla por formato de país
        assertThatThrownBy(() -> service.validateJsonStructure(json))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ISO Rule Violation: Invalid Country Code format");
    }

    @Test
    void validate_boundaryMinMax_returnsExpected() throws Exception {
        // Given: Nombre de 140 caracteres (válido)
        String name140 = "A".repeat(140);
        String json140 = "{" +
                "\"referenceData\": {" +
                "\"fullLegalName\": \"" + name140 + "\"," +
                "\"countryCode\": \"USA\"," +
                "\"townName\": \"Boston\"" +
                "}" +
                "}";
        JsonNode node140 = realMapper.readTree(json140);
        when(objectMapper.readTree(json140)).thenReturn(node140);

        // Then: No excepción para límite aceptado
        assertThatCode(() -> service.validateJsonStructure(json140))
                .doesNotThrowAnyException();

        // And: Nombre de 141 caracteres (inválido)
        String name141 = "B".repeat(141);
        String json141 = "{" +
                "\"referenceData\": {" +
                "\"fullLegalName\": \"" + name141 + "\"," +
                "\"countryCode\": \"US\"," +
                "\"townName\": \"Seattle\"" +
                "}" +
                "}";
        JsonNode node141 = realMapper.readTree(json141);
        when(objectMapper.readTree(json141)).thenReturn(node141);

        assertThatThrownBy(() -> service.validateJsonStructure(json141))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ISO Rule Violation: Name exceeds 140 chars");
    }

    @Test
    void validate_conditionalRule_returnsInvalidWhenMissing() throws Exception {
        // Given: countryCode presente pero vacío (valor faltante)
        String json = "{" +
                "\"referenceData\": {" +
                "\"fullLegalName\": \"Client X\"," +
                "\"countryCode\": \"\"," +
                "\"townName\": \"Quito\"" +
                "}" +
                "}";
        JsonNode node = realMapper.readTree(json);
        when(objectMapper.readTree(json)).thenReturn(node);

        // When / Then: Falla por formato (regla condicional sobre countryCode presente)
        assertThatThrownBy(() -> service.validateJsonStructure(json))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ISO Rule Violation: Invalid Country Code format");
    }

    @Test
    void validate_nullRequest_throwsOrReturnsInvalid() throws Exception {
        // Given: jsonPayload nulo
        // Mockito sólo permite checked exceptions declaradas en la firma.
        // Para readTree(String), lanzamos RuntimeException para simular fallo de parseo nulo.
        when(objectMapper.readTree((String) null)).thenThrow(new RuntimeException("JSON input is null"));

        // When / Then: Se transforma en IllegalArgumentException con prefijo de mensaje
        assertThatThrownBy(() -> service.validateJsonStructure(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Datos inválidos ISO 20022: JSON input is null");
    }
}

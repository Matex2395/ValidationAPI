package com.fisa.validationapi.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Crear un ObjectMapper personalizado
        ObjectMapper objectMapper = new ObjectMapper();

        // Registrar el módulo para soportar LocalDateTime, LocalDate, etc.
        objectMapper.registerModule(new JavaTimeModule());

        // Guardar fechas como texto ISO-8601 (ej: "2026-01-18T10:00:00")
        // en lugar de un array de números [2026, 1, 18, 10, 0]
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Habilitar el tipado dinámico para que Redis sepa qué clase Java recuperar
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // Configurar el Serializador de Redis usando el ObjectMapper
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // Asignar serializadores
        // Keys: Strings simples
        template.setKeySerializer(new StringRedisSerializer());
        // Values: JSON complejo
        template.setValueSerializer(serializer);

        // Aplicar lo mismo para Hashes
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}
package com.fisa.validationapi.infrastructure.config;

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

        // 1. Serializador para las CLAVES (Keys)
        // Uso de String para que la key sea legible (ej: "idempotency:ABC-123")
        template.setKeySerializer(new StringRedisSerializer());

        // 2. Serializador para los VALORES (Values)
        // Uso de Jackson para guardar el objeto como JSON
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        // 3. Serializadores para Hash (si se usan hashes en Redis)
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}
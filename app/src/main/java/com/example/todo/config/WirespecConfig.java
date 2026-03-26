package com.example.todo.config;

import com.example.todo.generated.model.ContentTypeSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import community.flock.wirespec.java.Wirespec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class WirespecConfig {

    @Bean
    @Primary
    public Wirespec.Serialization wirespecSerialization(ObjectMapper objectMapper) {
        return new ContentTypeSerializer(objectMapper);
    }
}

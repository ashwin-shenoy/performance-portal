package com.hamza.performanceportal.performance.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA Converter for PostgreSQL JSONB column type
 * Converts between Java Map and PostgreSQL JSONB using String representation
 * Hibernate will handle the JDBC type mapping automatically
 */
@Slf4j
@Component
@Converter(autoApply = false)
public class JsonbConverter implements AttributeConverter<Map<String, Object>, String> {

    private final ObjectMapper objectMapper;

    public JsonbConverter() {
        this.objectMapper = new ObjectMapper();
        // Configure ObjectMapper for better handling of various data types
        this.objectMapper.findAndRegisterModules();
    }

    /**
     * Convert Map to JSON String for database storage
     */
    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "{}";
        }
        
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Error converting Map to JSON String", e);
            throw new IllegalArgumentException("Error converting capability-specific data to JSONB", e);
        }
    }

    /**
     * Convert JSON String from database to Map
     */
    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty() || "{}".equals(dbData.trim())) {
            return new HashMap<>();
        }
        
        try {
            return objectMapper.readValue(dbData,
                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));
        } catch (IOException e) {
            log.error("Error converting JSON String to Map: {}", dbData, e);
            throw new IllegalArgumentException("Error converting JSONB to capability-specific data", e);
        }
    }
}

// Made with Bob
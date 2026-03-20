package com.discord.LocalAIDiscordAgent.objectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.lang.reflect.RecordComponent;
import java.util.*;

@Component
public class MapperUtils {

    private final ObjectMapper aiObjectMapper;
    private static final PropertyNamingStrategies.SnakeCaseStrategy SNAKE_CASE =
            new PropertyNamingStrategies.SnakeCaseStrategy();

    public MapperUtils(@Qualifier("aiObjectMapper") ObjectMapper aiObjectMapper) {
        this.aiObjectMapper = aiObjectMapper;
    }

    public String recordToString(Record record) {
        try {
            return aiObjectMapper.writeValueAsString(record);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize system message config", e);
        }
    }

    public String generateSchema(Record recordInstance) {
        try {
            Object rawTypeStructure = resolve(recordInstance);
            Object snakeCaseStructure = toSnakeCaseKeys(rawTypeStructure);
            return aiObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(snakeCaseStructure);
        } catch (Exception e) {
            return "{\"error\":\"Schema generation failed: " + e.getMessage() + "\"}";
        }
    }

    private Object resolve(Object value) {
        if (value == null) return null;

        if (value instanceof Record rec) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (RecordComponent comp : rec.getClass().getRecordComponents()) {
                try {
                    Object fieldValue = comp.getAccessor().invoke(rec);
                    map.put(comp.getName(), resolve(fieldValue));
                } catch (Exception e) {
                    map.put(comp.getName(), "Unknown");
                }
            }
            return map;
        }

        if (value instanceof Collection<?> col) {
            if (col.isEmpty()) return Collections.emptyList();

            List<Object> result = new ArrayList<>();
            result.add(resolve(col.iterator().next()));
            return result;
        }

        return value.getClass().getSimpleName();
    }

    private Object toSnakeCaseKeys(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> converted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                converted.put(SNAKE_CASE.translate(key), toSnakeCaseKeys(entry.getValue()));
            }
            return converted;
        }

        if (value instanceof Collection<?> col) {
            List<Object> converted = new ArrayList<>();
            for (Object item : col) {
                converted.add(toSnakeCaseKeys(item));
            }
            return converted;
        }

        return value;
    }
}
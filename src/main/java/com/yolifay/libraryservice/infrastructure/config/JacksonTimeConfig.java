package com.yolifay.libraryservice.infrastructure.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonTimeConfig {

    @Bean
    Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer(ZoneId appZoneId) {
        return builder -> {
            // Formatter ISO_OFFSET_DATE_TIME dengan zona dari properties
            DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(appZoneId);

            // Daftarkan serializer khusus untuk Instant
            builder.serializerByType(Instant.class, new JsonSerializer<Instant>() {
                @Override
                public void serialize(Instant instant, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
                    jsonGenerator.writeString(fmt.format(instant)); // contoh: 2025-08-23T21:00:57.800+07:00
                }
            });
            // (Opsional) Kalau mau tetap simpan default module lain:
//             builder.modulesToInstall(new JavaTimeModule());
        };
    }
}

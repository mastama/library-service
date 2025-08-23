package com.yolifay.libraryservice.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

@Configuration
public class TimeConfig {

    @Bean
    public ZoneId appZoneId(@Value("${app.time-zone:Asia/Jakarta}") String zone) {
        return ZoneId.of(zone);
    }
}

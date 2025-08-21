package com.yolifay.libraryservice.infrastructure.config;

import com.yolifay.libraryservice.domain.service.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

@Configuration
public class ClockConfig {
    @Bean
    public Clock systemClock(){ return Instant::now; }
}

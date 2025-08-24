package com.yolifay.libraryservice.infrastructure.ratelimit;

import com.yolifay.libraryservice.domain.service.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
@RequiredArgsConstructor
public class RateLimiterConfig implements WebMvcConfigurer {

    // Ini akan diinject ke RedisRateLimiter (@Component) yg sudah kamu buat
    private final RateLimiter limiter;
    private final RateLimitProperties props;

    @Bean(name = "rateLimitInterceptor")
    public RateLimitInterceptor rateLimitInterceptor() {
        return new RateLimitInterceptor(limiter, props);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor());
    }
}
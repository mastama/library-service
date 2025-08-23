package com.yolifay.libraryservice.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // REST stateless
                .authorizeHttpRequests(auth -> auth
                        // buka swagger (opsional)
                        .requestMatchers("/swagger", "/swagger/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        // contoh: GET library boleh tanpa auth, lainnya perlu auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/library/**").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults()); // Basic Auth
        return http.build();
    }
}

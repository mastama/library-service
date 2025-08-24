package com.yolifay.libraryservice.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtFilter;
    private final JsonAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthFilter jwtFilter, JsonAccessDeniedHandler accessDeniedHandler) {
        this.jwtFilter = jwtFilter;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ===== Auth (public) =====
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/request-otp",   // jika pakai MFA
                                "/api/v1/auth/login-otp"      // jika pakai MFA
                        ).permitAll()

                        // Logout wajib sudah terautentikasi (bawa Bearer)
                        .requestMatchers("/api/v1/auth/logout").authenticated()

                        // ===== Swagger (public) =====
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/swagger/**").permitAll()

                        // ===== Users (hanya Super Admin) =====
                        .requestMatchers("/api/v1/users/**").hasRole("SUPER_ADMIN")

                        // ===== Audit Logs (hanya Super Admin) =====
                        .requestMatchers("/api/v1/audit-logs/**").hasRole("SUPER_ADMIN")

                        // ===== Articles (RBAC per method) =====
                        // Viewer hanya boleh lihat
                        .requestMatchers(HttpMethod.GET, "/api/v1/articles/**")
                        .hasAnyRole("SUPER_ADMIN","EDITOR","CONTRIBUTOR","VIEWER")
                        // Create & Update: SuperAdmin/Editor/Contributor
                        .requestMatchers(HttpMethod.POST, "/api/v1/articles/**")
                        .hasAnyRole("SUPER_ADMIN","EDITOR","CONTRIBUTOR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/articles/**")
                        .hasAnyRole("SUPER_ADMIN","EDITOR","CONTRIBUTOR")
                        // Delete: SuperAdmin bisa semua, Editor hanya miliknya (dicek lagi di @artSec.canDelete)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/articles/**")
                        .hasAnyRole("SUPER_ADMIN","EDITOR")

                        // ===== Library contoh: GET publik, lainnya butuh auth =====
                        .requestMatchers(HttpMethod.GET, "/api/v1/library/**").permitAll()

                        // Sisa endpoint: wajib autentikasi
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(accessDeniedHandler) // custom 403 JSON
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                // Filter JWT sebelum UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                // Basic auth optional untuk debug
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}

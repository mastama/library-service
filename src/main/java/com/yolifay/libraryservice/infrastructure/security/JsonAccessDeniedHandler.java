package com.yolifay.libraryservice.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    // Pakai ObjectMapper global agar konsisten dengan konfigurasi Jackson kamu (Instant serializer, dll.)
    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        // Ambil info user dari SecurityContext
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        String role = null;
        if (auth != null) {
            Object details = auth.getDetails();
            if (details instanceof Long l) userId = l;
            role = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst().orElse(null);
        }

        // Body JSON ramah klien
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 403);
        body.put("error", "FORBIDDEN");
        body.put("message", accessDeniedException.getMessage() != null
                ? accessDeniedException.getMessage() : "Access is denied");
        body.put("path", request.getRequestURI());
        body.put("method", request.getMethod());
        body.put("timestamp", Instant.now()); // akan ikut serializer global kamu
        body.put("userId", userId);
        body.put("role", role);

        // Log jelas untuk troubleshooting
        log.warn("[SEC-403] userId={} role={} {} {} -> {}", userId, role,
                request.getMethod(), request.getRequestURI(), body.get("message"));

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

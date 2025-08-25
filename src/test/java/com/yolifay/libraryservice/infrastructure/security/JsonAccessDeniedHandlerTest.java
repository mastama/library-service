package com.yolifay.libraryservice.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JsonAccessDeniedHandlerTest {

    private JsonAccessDeniedHandler handler;
    private ObjectMapper objectMapper;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private ByteArrayOutputStream responseBodyStream;

    @BeforeEach
    void setUp() throws IOException {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // <-- Tambahkan ini
        handler = new JsonAccessDeniedHandler(objectMapper);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        responseBodyStream = new ByteArrayOutputStream();
        ServletOutputStream servletOutputStream = new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {

            }

            @Override
            public void write(int b) {
                responseBodyStream.write(b);
            }
        };
        when(response.getOutputStream()).thenReturn(servletOutputStream);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testHandle_WithAuthenticatedUser_Positive() throws IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/books");
        when(request.getMethod()).thenReturn("GET");
        AccessDeniedException ex = new AccessDeniedException("Not allowed");

        var auth = new TestingAuthenticationToken("user", "password",
                String.valueOf(new SimpleGrantedAuthority("ROLE_ADMIN")));
        auth.setDetails(123L); // set userId
        SecurityContextHolder.getContext().setAuthentication(auth);

        // When
        handler.handle(request, response, ex);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json");

        String jsonResponse = responseBodyStream.toString();
        Map<?, ?> result = objectMapper.readValue(jsonResponse, Map.class);

        assertEquals(403, result.get("status"));
        assertEquals("FORBIDDEN", result.get("error"));
        assertEquals("Not allowed", result.get("message"));
        assertEquals("/api/books", result.get("path"));
        assertEquals("GET", result.get("method"));
        assertEquals(123, result.get("userId"));
        assertEquals("ROLE_ADMIN", result.get("role"));
        assertNotNull(result.get("timestamp"));
    }

    @Test
    void testHandle_WithoutAuthAndNullMessage_Negative() throws IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/secure");
        when(request.getMethod()).thenReturn("POST");
        AccessDeniedException ex = new AccessDeniedException(null); // no custom message

        SecurityContextHolder.clearContext(); // auth = null

        // When
        handler.handle(request, response, ex);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json");

        String jsonResponse = responseBodyStream.toString();
        Map<?, ?> result = objectMapper.readValue(jsonResponse, Map.class);

        assertEquals(403, result.get("status"));
        assertEquals("FORBIDDEN", result.get("error"));
        assertEquals("Access is denied", result.get("message")); // default message
        assertEquals("/api/secure", result.get("path"));
        assertEquals("POST", result.get("method"));
        assertNull(result.get("userId"));
        assertNull(result.get("role"));
        assertNotNull(result.get("timestamp"));
    }

    @Test
    void testHandle_WithAuthButNoAuthorities_Negative() throws IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/empty");
        when(request.getMethod()).thenReturn("PUT");
        AccessDeniedException ex = new AccessDeniedException("No authority");

        var auth = new TestingAuthenticationToken("user", "password");
        auth.setDetails(456L);
        SecurityContextHolder.getContext().setAuthentication(auth); // no roles

        // When
        handler.handle(request, response, ex);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json");

        String jsonResponse = responseBodyStream.toString();
        Map<?, ?> result = objectMapper.readValue(jsonResponse, Map.class);

        assertEquals(403, result.get("status"));
        assertEquals("FORBIDDEN", result.get("error"));
        assertEquals("No authority", result.get("message"));
        assertEquals("/api/empty", result.get("path"));
        assertEquals("PUT", result.get("method"));
        assertEquals(456, result.get("userId"));
        assertNull(result.get("role")); // no authority assigned
        assertNotNull(result.get("timestamp"));
    }
}


package com.yolifay.libraryservice.infrastructure.security;

import com.yolifay.libraryservice.domain.service.TokenIssuer;
import com.yolifay.libraryservice.domain.service.TokenStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    private TokenIssuer tokenIssuer;
    private TokenStore tokenStore;
    private JwtAuthFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        tokenIssuer = mock(TokenIssuer.class);
        tokenStore = mock(TokenStore.class);
        filter = new JwtAuthFilter(tokenIssuer, tokenStore);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDoFilterInternal_withValidBearerTokenAndWhitelisted() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer validtoken");
        when(request.getRequestURI()).thenReturn("/api/books");
        var decoded = new TokenIssuer.DecodedToken(123L, "user1", "email@test.com", "ADMIN", "jti123");
        when(tokenIssuer.verify("validtoken")).thenReturn(decoded);
        when(tokenStore.isWhitelisted("jti123")).thenReturn(true);

        // Act
        filter.doFilterInternal(request, response, chain);

        // Assert
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("user1", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(chain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_withValidBearerTokenButNotWhitelisted() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer validtoken");
        when(request.getRequestURI()).thenReturn("/api/books");
        var decoded = new TokenIssuer.DecodedToken(123L, "user1", "email@test.com", "USER", "jti123");
        when(tokenIssuer.verify("validtoken")).thenReturn(decoded);
        when(tokenStore.isWhitelisted("jti123")).thenReturn(false);

        // Act
        filter.doFilterInternal(request, response, chain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_withExpiredJwtException() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer expiredtoken");
        when(request.getRequestURI()).thenReturn("/api/books");
        io.jsonwebtoken.ExpiredJwtException expired = mock(io.jsonwebtoken.ExpiredJwtException.class);
        when(expired.getClaims()).thenReturn(mock(io.jsonwebtoken.Claims.class));
        when(tokenIssuer.verify("expiredtoken")).thenThrow(expired);

        // Act
        filter.doFilterInternal(request, response, chain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_withInvalidToken() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer invalidtoken");
        when(request.getRequestURI()).thenReturn("/api/books");
        when(tokenIssuer.verify("invalidtoken")).thenThrow(new RuntimeException("Invalid signature"));

        // Act
        filter.doFilterInternal(request, response, chain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_withNoAuthorizationHeader() throws ServletException, IOException {
        // Arrange
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/books");

        // Act
        filter.doFilterInternal(request, response, chain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }
}
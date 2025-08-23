package com.yolifay.libraryservice.infrastructure.security;

import com.yolifay.libraryservice.domain.service.TokenIssuer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final TokenIssuer tokenIssuer;
    public JwtAuthFilter(TokenIssuer tokenIssuer){ this.tokenIssuer = tokenIssuer; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                var dec = tokenIssuer.verify(token);
                var auth = new UsernamePasswordAuthenticationToken(
                        dec.username(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                auth.setDetails(dec.userId());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) { /* invalid token → continue without auth */ }
        }
        filterChain.doFilter(request, response);
    }
}

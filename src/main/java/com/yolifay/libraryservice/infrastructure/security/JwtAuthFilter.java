package com.yolifay.libraryservice.infrastructure.security;

import com.yolifay.libraryservice.domain.service.TokenIssuer;
import com.yolifay.libraryservice.domain.service.TokenStore;
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
    private final TokenStore tokenStore;

    public JwtAuthFilter(TokenIssuer tokenIssuer, TokenStore tokenStore) {
        this.tokenIssuer = tokenIssuer;
        this.tokenStore = tokenStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                var dec = tokenIssuer.verify(token);
                // token harus ada di whitelist Redis
                if (tokenStore.isWhitelisted(dec.jti())) {
                    var auth = new UsernamePasswordAuthenticationToken(
                            dec.username(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                    auth.setDetails(dec.userId()); // simpan userId
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) { /* invalid → no auth */ }
        }
        chain.doFilter(req, res);
    }
}

package com.yolifay.libraryservice.infrastructure.security;

import com.yolifay.libraryservice.domain.service.TokenIssuer;
import com.yolifay.libraryservice.domain.service.TokenStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
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
        String uri = req.getRequestURI();

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                var dec = tokenIssuer.verify(token);
                // 1. cek token harus ada di whitelist Redis
                if (tokenStore.isWhitelisted(dec.jti())) {
                    var authority = new SimpleGrantedAuthority("ROLE_" + dec.role());
                    var auth = new UsernamePasswordAuthenticationToken(
                            dec.username(), null, List.of(authority));
                    auth.setDetails(dec.userId()); // simpan userId untuk CurrentUser.id()
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("[AUTH] accepted jti={} userId={} role={} uri={}", dec.jti(), dec.userId(), dec.role(), uri);
                } else {
                    log.warn("[AUTH] jti not whitelisted (revoked/expired in Redis), uri={}, jti={}", uri, dec.jti());
                }
            } catch (io.jsonwebtoken.ExpiredJwtException ex) {
                log.warn("[AUTH] expired token on uri={}, expAt={}", uri, ex.getClaims().getExpiration());
            } catch (Exception ex) {
                log.warn("[AUTH] invalid token on uri={}, err={}", uri, ex.toString());
            }
        } else {
            log.trace("[AUTH] no bearer header for uri={}", uri);
        }

        chain.doFilter(req, res);
    }
}

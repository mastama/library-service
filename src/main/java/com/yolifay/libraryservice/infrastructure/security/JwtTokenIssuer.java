package com.yolifay.libraryservice.infrastructure.security;

import com.yolifay.libraryservice.domain.service.TokenIssuer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenIssuer implements TokenIssuer {

    private final SecretKey key;
    private final String issuer;
    private final long accessExpMinutes;

    public JwtTokenIssuer(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer:library-service}") String issuer,
            @Value("${jwt.access-expiration-minutes:60}") long accessExpMinutes
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.issuer = issuer;
        this.accessExpMinutes = accessExpMinutes;
    }

    @Override
    public Token issue(Long userId, String username, String email, String fullName,
                       com.yolifay.libraryservice.domain.model.Role role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessExpMinutes * 60);
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .setId(jti)                                // jti
                .setSubject(userId.toString())             // sub
                .setIssuer(issuer)                         // iss
                .setIssuedAt(Date.from(now))               // iat
                .setExpiration(Date.from(exp))             // exp
                .claim("username", username)
                .claim("email", email)
                .claim("fullName", fullName)
                .claim("role", role.name())                // <-- ROLE ke dalam token
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        log.info("[JWT] issued jti={} userId={} role={} expAt={}", jti, userId, role, exp);
        return new Token(token, jti, now, exp);
    }

    @Override
    public DecodedToken verify(String token) {
        try {
            Claims c = Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody();

            var dec = new DecodedToken(
                    Long.valueOf(c.getSubject()),
                    c.get("username", String.class),
                    c.get("email", String.class),
                    c.get("role", String.class),
                    c.getId()
            );
            log.debug("[JWT] verified jti={} userId={} role={}", dec.jti(), dec.userId(), dec.role());
            return dec;

        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            log.warn("[JWT] token expired at {}", ex.getClaims().getExpiration());
            throw ex;
        } catch (Exception ex) {
            log.warn("[JWT] token invalid: {}", ex.toString());
            throw ex;
        }
    }
}
package com.yolifay.libraryservice.infrastructure.security;

import com.yolifay.libraryservice.domain.service.TokenIssuer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenIssuer implements TokenIssuer {

    private final SecretKey key;
    private final String issuer;
    private final long expirationMinutes;

    public JwtTokenIssuer(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer:library-service}") String issuer,
            @Value("${jwt.expiration-minutes:60}") long expirationMinutes
    ) {
        // pastikan secret >= 32 chars utk HS256
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
    }

    @Override
    public String issue(Long userId, String username, String email, String fullName) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(userId.toString()) // simpan userId di subject
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
                .claim("username", username)
                .claim("email", email)
                .claim("fullName", fullName)
                .signWith(key, SignatureAlgorithm.HS256)  // 0.11.5 style
                .compact();
    }

    @Override
    public DecodedToken verify(String token) {
        // 0.11.5 parser style
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Long userId = Long.valueOf(claims.getSubject()); // ambil dari subject
        String username = claims.get("username", String.class);
        String email    = claims.get("email", String.class);

        return new DecodedToken(userId, username, email);
    }
}
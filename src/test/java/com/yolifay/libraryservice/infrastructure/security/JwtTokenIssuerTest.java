package com.yolifay.libraryservice.infrastructure.security;

import com.yolifay.libraryservice.domain.model.Role;
import com.yolifay.libraryservice.domain.service.TokenIssuer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenIssuerTest {

    private static final String SECRET = "01234567890123456789012345678901"; // 32 chars
    private JwtTokenIssuer issuer;

    @BeforeEach
    void setUp() {
        // issuer = "test-issuer", masa berlaku access = 2 menit
        issuer = new JwtTokenIssuer(SECRET, "test-issuer", 2L);
    }

    @Test
    void issue_and_verify_success_allClaimsAndTtlCorrect() {
        long userId = 7L;

        TokenIssuer.Token tok = issuer.issue(userId, "alice", "alice@example.test", "Alice", Role.EDITOR);

        assertNotNull(tok.value());
        assertNotNull(tok.jti());

        // parse secara mandiri untuk cek klaim
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Claims c = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(tok.value()).getBody();

        // cek klaim standar & custom
        assertEquals(tok.jti(), c.getId());
        assertEquals(String.valueOf(userId), c.getSubject());
        assertEquals("test-issuer", c.getIssuer());
        assertEquals("alice", c.get("username"));
        assertEquals("alice@example.test", c.get("email"));
        assertEquals("Alice", c.get("fullName"));
        assertEquals("EDITOR", c.get("role"));

        // TTL ~ 120 detik (beri toleransi beberapa detik)
        long ttl = Duration.between(tok.issuedAt(), tok.expiresAt()).getSeconds();
        assertTrue(ttl >= 120 && ttl <= 125, "TTL harus sekitar 120 detik");

        // verify() mengembalikan decoded token sesuai
        TokenIssuer.DecodedToken dec = issuer.verify(tok.value());
        assertEquals(userId, dec.userId());
        assertEquals("alice", dec.username());
        assertEquals("alice@example.test", dec.email());
        assertEquals("EDITOR", dec.role());
        assertEquals(tok.jti(), dec.jti());
    }

    @Test
    void verify_expiredToken_throwsExpiredJwtException() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Instant now = Instant.now();
        // exp di masa lalu
        String expiredJwt = Jwts.builder()
                .setId("jti-expired")
                .setSubject("9")
                .setIssuer("test-issuer")
                .setIssuedAt(Date.from(now.minusSeconds(120)))
                .setExpiration(Date.from(now.minusSeconds(60)))
                .claim("username", "u")
                .claim("email", "u@mail")
                .claim("fullName", "U")
                .claim("role", "VIEWER")
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertThrows(ExpiredJwtException.class, () -> issuer.verify(expiredJwt));
    }

    @Test
    void verify_invalidSignature_throwsJwtException() {
        // token ditandatangani dengan secret berbeda → signature mismatch
        SecretKey wrong = Keys.hmacShaKeyFor("abcdefghijklmnopqrstuvwx12345678".getBytes());
        Instant now = Instant.now();
        String badJwt = Jwts.builder()
                .setId("jti-bad")
                .setSubject("3")
                .setIssuer("test-issuer")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(600)))
                .claim("username", "x")
                .claim("email", "x@mail")
                .claim("fullName", "X")
                .claim("role", "EDITOR")
                .signWith(wrong, SignatureAlgorithm.HS256)
                .compact();

        assertThrows(JwtException.class, () -> issuer.verify(badJwt));
    }
}

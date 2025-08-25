package com.yolifay.libraryservice.domain.service;

import com.yolifay.libraryservice.domain.model.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TokenIssuerTest {

    /**
     * Test-double sederhana untuk memenuhi kontrak TokenIssuer.
     * Tidak ada dependensi eksternal/JWT beneran, cukup format string.
     *
     * Format token: "token-<userId>-<jti>"
     */
    static class FakeTokenIssuer implements TokenIssuer {

        @Override
        public Token issue(Long userId, String username, String email, String fullName, Role role) {
            if (userId == null || username == null || email == null || fullName == null || role == null) {
                throw new IllegalArgumentException("null param");
            }
            Instant now = Instant.now();
            String jti = UUID.randomUUID().toString();
            String value = "token-" + userId + "-" + jti;
            return new Token(value, jti, now, now.plusSeconds(3600));
        }

        @Override
        public DecodedToken verify(String token) {
            if (token == null || token.isBlank()) {
                throw new IllegalArgumentException("blank token");
            }
            // Pisahkan maksimum 3 bagian agar UUID yang mengandung '-' tetap utuh di bagian ke-3
            String[] parts = token.split("-", 3);
            if (parts.length < 3 || !parts[0].equals("token")) {
                throw new IllegalArgumentException("bad token");
            }
            Long userId = Long.valueOf(parts[1]);
            String jti = parts[2];
            // Username/email dummy untuk validasi round-trip
            return new DecodedToken(userId, "user-" + userId, "user" + userId + "@example.test", "USER", jti);
        }
    }

    private final TokenIssuer issuer = new FakeTokenIssuer();

    // ====== POSITIVE PATHS ======

    @Test
    void issue_success_and_verify_roundtrip() {
        TokenIssuer.Token tok =
                issuer.issue(7L, "alice", "alice@example.test", "Alice A", Role.EDITOR);

        assertNotNull(tok);
        assertTrue(tok.value().startsWith("token-7-"));
        assertNotNull(tok.jti());
        assertNotNull(tok.issuedAt());
        assertNotNull(tok.expiresAt());
        assertTrue(tok.expiresAt().isAfter(tok.issuedAt()));

        TokenIssuer.DecodedToken dec = issuer.verify(tok.value());
        assertEquals(7L, dec.userId());
        assertEquals("USER", dec.role());
        assertEquals(tok.jti(), dec.jti());
        assertEquals("user-7", dec.username());
        assertEquals("user7@example.test", dec.email());
    }

    @Test
    void records_accessors_equals_hashcode_tostring() {
        Instant now = Instant.now();
        TokenIssuer.Token t1 = new TokenIssuer.Token("v", "j", now, now.plusSeconds(1));
        TokenIssuer.Token t2 = new TokenIssuer.Token("v", "j", now, now.plusSeconds(1));

        assertEquals("v", t1.value());
        assertEquals("j", t1.jti());
        assertTrue(t1.expiresAt().isAfter(t1.issuedAt()));
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
        assertTrue(t1.toString().contains("v"));

        TokenIssuer.DecodedToken d1 =
                new TokenIssuer.DecodedToken(1L, "u", "e", "ADMIN", "jid");
        TokenIssuer.DecodedToken d2 =
                new TokenIssuer.DecodedToken(1L, "u", "e", "ADMIN", "jid");

        assertEquals(1L, d1.userId());
        assertEquals("u", d1.username());
        assertEquals("e", d1.email());
        assertEquals("ADMIN", d1.role());
        assertEquals("jid", d1.jti());
        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());
        assertTrue(d1.toString().contains("ADMIN"));
    }

    // ====== NEGATIVE PATHS ======

    @Test
    void issue_nullParam_throws() {
        // Cukup satu kasus null sudah mengeksekusi cabang guard
        assertThrows(IllegalArgumentException.class,
                () -> issuer.issue(null, "u", "e", "Full", Role.VIEWER));
        assertThrows(IllegalArgumentException.class,
                () -> issuer.issue(1L, null, "e", "Full", Role.SUPER_ADMIN));
        assertThrows(IllegalArgumentException.class,
                () -> issuer.issue(1L, "u", "e", "Full", null));
    }

    @Test
    void verify_blank_or_malformed_token_throws() {
        assertThrows(IllegalArgumentException.class, () -> issuer.verify(null));
        assertThrows(IllegalArgumentException.class, () -> issuer.verify(""));
        assertThrows(IllegalArgumentException.class, () -> issuer.verify("token-123"));     // kurang jti
        assertThrows(IllegalArgumentException.class, () -> issuer.verify("badprefix-1-abc"));// prefix salah
    }
}

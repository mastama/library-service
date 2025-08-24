package com.yolifay.libraryservice.domain.service;

import com.yolifay.libraryservice.domain.model.Role;

import java.time.Instant;

public interface TokenIssuer {
    Token issue(Long userId, String username, String email, String fullName, Role role); // create JWT + meta
    DecodedToken verify(String token);                                        // parse + verify

    record Token(String value, String jti, Instant issuedAt, Instant expiresAt) {}
    record DecodedToken(Long userId, String username, String email, String role, String jti) {}
}

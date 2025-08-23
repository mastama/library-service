package com.yolifay.libraryservice.application.dto.auth;

import java.time.Instant;

public record TokenResponse (
        String token,
        Instant issuedAt,
        Instant expiresAt
){
}

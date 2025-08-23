package com.yolifay.libraryservice.application.dto.auth;

import java.time.Instant;

public record TokenPairResponse(
        String accessToken,
        Instant accessIssuedAt,
        Instant accessExpiresAt,
        String refreshToken,
        Instant refreshIssuedAt
) {
}

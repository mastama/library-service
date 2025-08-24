package com.yolifay.libraryservice.domain.usecase.auth.handler;

import com.yolifay.libraryservice.application.dto.auth.TokenPairResponse;
import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.service.PasswordHasher;
import com.yolifay.libraryservice.domain.service.RefreshTokenStore;
import com.yolifay.libraryservice.domain.service.TokenIssuer;
import com.yolifay.libraryservice.domain.service.TokenStore;
import com.yolifay.libraryservice.domain.usecase.auth.command.LoginUser;
import com.yolifay.libraryservice.infrastructure.ratelimit.RateLimitGuard;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class LoginUserHandler {

    private final UserRepositoryPort users;
    private final PasswordHasher hasher;
    private final TokenIssuer tokenIssuer;
    private final TokenStore accessWhitelist;
    private final RefreshTokenStore refreshStore;

    // rate limiter
    private final RateLimitGuard rl;
    private final HttpServletRequest httpServletRequest;

    // injeksi dari properties (Duration -> bisa "14d", "48h", dll.)
    @Value("${jwt.refresh-expiration:14d}")
    private Duration refreshExp;

    /** Kembalikan TokenPairResponse langsung untuk dipakai controller. */
    public TokenPairResponse execute(LoginUser c) {
        // rate limit per IP (+ identity username/email)
        rl.check("login", httpServletRequest, null, c.usernameOrEmail());

        User u = users.findByUsernameOrEmail(c.usernameOrEmail().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!hasher.matches(c.password(), u.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        // 2) issie Access token (JWT) + whitelist di Redis
        var access = tokenIssuer.issue(u.getId(), u.getUsername(), u.getEmail(), u.getFullName(), u.getRole());
        var accessTtl = Duration.between(access.issuedAt(), access.expiresAt());
        accessWhitelist.whitelist(access.jti(), u.getId(), accessTtl);

        // 3) Refresh token (opaque string di Redis; rotate di endpoint /refresh)
        String refresh = refreshStore.issue(u.getId(), refreshExp);
        Instant refreshExpAt = Instant.now().plus(refreshExp);

        // 4) Response
        return new TokenPairResponse(
                access.value(), access.issuedAt(), access.expiresAt(),
                refresh, refreshExpAt
        );
    }
}

package com.yolifay.libraryservice.domain.usecase.auth.handler;

import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.service.RefreshTokenStore;
import com.yolifay.libraryservice.domain.service.TokenIssuer;
import com.yolifay.libraryservice.domain.service.TokenStore;
import com.yolifay.libraryservice.domain.usecase.auth.command.RefreshAccessToken;
import com.yolifay.libraryservice.infrastructure.ratelimit.RateLimitGuard;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshAccessTokenHandler {

    private final RefreshTokenStore refreshStore;
    private final UserRepositoryPort users;
    private final TokenIssuer tokenIssuer;
    private final TokenStore tokenWhitelist;

    private final RateLimitGuard rl;
    private final HttpServletRequest httpServletRequest;

    @Value("${jwt.refresh-expiration-days:14}")
    public Long refreshDays; // fallback, properti dipakai di controller

    public TokenPair execute(RefreshAccessToken c) {

        // 1) consume refresh token (sekali pakai)
        Long userId = refreshStore.consume(c.refreshToken());
        // lihat userId dari refresh token (tanpa rotate dulu)
        rl.check("refresh", httpServletRequest, userId, null);

        if (userId == null) throw new IllegalArgumentException("Invalid refresh token");

        // 2) muat user utk claim JWT
        User u = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 3) issue access token baru
        var access = tokenIssuer.issue(u.getId(), u.getUsername(), u.getEmail(), u.getFullName(), u.getRole());
        var ttl = Duration.between(access.issuedAt(), access.expiresAt());
        tokenWhitelist.whitelist(access.jti(), u.getId(), ttl);

        // 4) issue refresh token baru (rotate)
        String newRefresh = refreshStore.issue(u.getId(), Duration.ofDays(refreshDays));

        return new TokenPair(access, newRefresh);
    }

    public record TokenPair(TokenIssuer.Token accessToken, String refreshToken) {}
}

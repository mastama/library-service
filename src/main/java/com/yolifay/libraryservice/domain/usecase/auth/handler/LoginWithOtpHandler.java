package com.yolifay.libraryservice.domain.usecase.auth.handler;

import com.yolifay.libraryservice.application.dto.auth.TokenPairResponse;
import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.service.*;
import com.yolifay.libraryservice.domain.usecase.auth.command.LoginWithOtp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginWithOtpHandler{
    private final UserRepositoryPort users;
    private final PasswordHasher hasher;
    private final OtpStore otpStore;
    private final TokenIssuer tokenIssuer;
    private final TokenStore accessWhitelist;
    private final RefreshTokenStore refreshStore;
    private final LoginAttemptService attempts; // lockout

    @Value("${jwt.refresh-expiration:14d}")
    private Duration refreshExp;

    public TokenPairResponse execute(LoginWithOtp c, String ip, String userAgent) {
        // 1) Ambil user/cek lockout
        User u = users.findByUsernameOrEmail(c.usernameOrEmail().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (attempts.isBlocked(u.getId())) {
            long left = attempts.blockSecondsLeft(u.getId());
            log.warn("[AUTH] login blocked userId={}, secondsLeft={}, ip={}, ua={}", u.getId(), left, ip, userAgent);
            throw new IllegalArgumentException("Account is temporarily blocked. Try again later.");
        }

        // 2) Verifikasi password
        if (!hasher.matches(c.password(), u.getPasswordHash())) {
            int count = attempts.onFailure(u.getId());
            log.warn("[AUTH] login failed (bad password) userId={}, failedCount={}, ip={}, ua={}", u.getId(), count, ip, userAgent);
            throw new IllegalArgumentException("Invalid credentials");
        }

        // 3) Verifikasi OTP
        String key = "login:" + u.getId();
        String code = otpStore.get(key);
        if (code == null || !code.equals(c.otp())) {
            int count = attempts.onFailure(u.getId());
            log.warn("[AUTH] login failed (bad otp) userId={}, failedCount={}, ip={}, ua={}", u.getId(), count, ip, userAgent);
            throw new IllegalArgumentException("Invalid OTP");
        }
        otpStore.remove(key);

        // 4) Sukses → reset counter & terbitkan token
        attempts.onSuccess(u.getId());
        var access = tokenIssuer.issue(u.getId(), u.getUsername(), u.getEmail(), u.getFullName(), u.getRole());
        accessWhitelist.whitelist(access.jti(), u.getId(), Duration.between(access.issuedAt(), access.expiresAt()));

        String refresh = refreshStore.issue(u.getId(), refreshExp);
        Instant refreshExpAt = Instant.now().plus(refreshExp);
        log.info("[AUTH] login success userId={}, role={}, jti={}, ip={}, ua={}",
                u.getId(), u.getRole(), access.jti(), ip, userAgent);

        return new TokenPairResponse(access.value(), access.issuedAt(), access.expiresAt(), refresh, refreshExpAt);
    }
}

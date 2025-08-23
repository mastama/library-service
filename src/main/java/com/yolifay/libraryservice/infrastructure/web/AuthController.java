package com.yolifay.libraryservice.infrastructure.web;

import com.yolifay.libraryservice.application.dto.auth.*;
import com.yolifay.libraryservice.domain.service.RefreshTokenStore;
import com.yolifay.libraryservice.domain.service.TokenIssuer;
import com.yolifay.libraryservice.domain.service.TokenStore;
import com.yolifay.libraryservice.domain.usecase.auth.command.LoginUser;
import com.yolifay.libraryservice.domain.usecase.auth.command.RefreshAccessToken;
import com.yolifay.libraryservice.domain.usecase.auth.command.RegisterUser;
import com.yolifay.libraryservice.domain.usecase.auth.handler.LoginUserHandler;
import com.yolifay.libraryservice.domain.usecase.auth.handler.RefreshAccessTokenHandler;
import com.yolifay.libraryservice.domain.usecase.auth.handler.RegisterUserHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final RegisterUserHandler registerHandler;
    private final LoginUserHandler loginHandler;
    private final TokenIssuer tokenIssuer;
    private final TokenStore accessWhitelist;
    private final RefreshAccessTokenHandler refreshHandler;
    private final RefreshTokenStore refreshStore;

    @Value("${jwt.refresh-expiration-days:14}")
    private long refreshDays;

    // Implement endpoints for registration and login
    @PostMapping("/register")
    public Long register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Incoming registering user with usernameOrEmail: {} - {}", registerRequest.username(), registerRequest.email());
        log.info("Outgoing registered user with usernameOrEmail: {} - {}", registerRequest.username(), registerRequest.email());
        return registerHandler.executeRegisterUser(new RegisterUser(
                registerRequest.fullName(),
                registerRequest.username(),
                registerRequest.email(),
                registerRequest.password()
        ));
    }

    @PostMapping("/login")
    public TokenPairResponse login(@Valid @RequestBody LoginRequest lr) {
        log.info("Incoming login user with usernameOrEmail: {}", lr.usernameOrEmail());
        log.info("Outgoing login user with usernameOrEmail: {}", lr.usernameOrEmail());
        return loginHandler.execute(new LoginUser(lr.usernameOrEmail(), lr.password()));
    }

    @PostMapping("/refresh")
    public TokenPairResponse refresh(@Valid @RequestBody RefreshRequest req){
        log.info("Incoming refresh access token");
        var pair = refreshHandler.execute(new RefreshAccessToken(req.refreshToken()));
        Instant refreshExp = Instant.now().plus(Duration.ofDays(refreshDays));

        log.info("Outgoing refresh access token success");
        return new TokenPairResponse(
                pair.accessToken().value(),
                pair.accessToken().issuedAt(),
                pair.accessToken().expiresAt(),
                pair.refreshToken(),
                refreshExp
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestBody(required = false) RefreshRequest req) {
        log.info("Incoming logout request");
        boolean accessRevoked = false;
        boolean refreshRevoked = false;

        // Revoke access token yang dipakai saat ini (wajib Bearer)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Secara normal tidak akan sampai ke sini kalau SecurityConfig sudah mewajibkan auth.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var dec = tokenIssuer.verify(authHeader.substring(7));
        accessWhitelist.revoke(dec.jti());
        accessRevoked = true;

        // Revoke refresh token kalau dikirim (via body atau cookie)
        if (req != null && req.refreshToken() != null && !req.refreshToken().isBlank()) {
            refreshStore.revoke(req.refreshToken());
            refreshRevoked = true;
        }

        log.info("Outgoing logout request success");
        return ResponseEntity.ok(new LogoutResponse(accessRevoked, refreshRevoked));
    }
}

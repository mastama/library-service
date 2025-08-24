package com.yolifay.libraryservice.infrastructure.web;

import com.yolifay.libraryservice.application.dto.auth.*;
import com.yolifay.libraryservice.domain.model.Role;
import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.service.RefreshTokenStore;
import com.yolifay.libraryservice.domain.service.TokenIssuer;
import com.yolifay.libraryservice.domain.service.TokenStore;
import com.yolifay.libraryservice.domain.usecase.auth.command.*;
import com.yolifay.libraryservice.domain.usecase.auth.handler.*;
import com.yolifay.libraryservice.infrastructure.audit.Audited;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.Optional;

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

    private final RequestOtpHandler requestOtpHandler;
    private final LoginWithOtpHandler loginWithOtpHandler;

    @Value("${mfa.otp.ttl:5m}")
    private java.time.Duration otpTtl;

    private final UserRepositoryPort users; // inject via constructor

    @Value("${jwt.refresh-expiration-days:14}")
    private long refreshDays;

    // Implement endpoints for registration and login
    @PostMapping("/register")
    public Long register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Incoming registering user with usernameOrEmail: {} - {}", registerRequest.username(), registerRequest.email());
        var role = parseRoleOrDefault(registerRequest.role());
        log.info("Outgoing registered user with usernameOrEmail: {} - {}", registerRequest.username(), registerRequest.email());
        return registerHandler.executeRegisterUser(new RegisterUser(
                registerRequest.fullName(),
                registerRequest.username(),
                registerRequest.email(),
                registerRequest.password(),
                role
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

    private Role parseRoleOrDefault(String s){
        if (s == null || s.isBlank()) return Role.VIEWER;
        try { return Role.valueOf(s.trim().toUpperCase()); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Invalid role"); }
    }

    @PostMapping("/request-otp")
    @Audited(action = "REQUEST_OTP")
    public ResponseEntity<OtpResponse> requestOtp(@Valid @RequestBody RequestOtpRequest r){
        log.info("Incoming request OTP for {}", r.usernameOrEmail());

        // jalankan use case (generate + kirim email via Mailpit)
        requestOtpHandler.execute(new OtpRequest(r.usernameOrEmail()));

        // cari email user utk ditampilkan (dimask)
        String email = users.findByUsernameOrEmail(r.usernameOrEmail().toLowerCase())
                .map(User::getEmail)
                .orElse("-");

        OtpResponse body = new OtpResponse(
                "OTP has been sent. Please check your email.",
                "email",
                maskEmail(email),
                otpTtl.toSeconds()
        );

        log.info("Outgoing request OTP success for {} (to={})", r.usernameOrEmail(), body.to());
        // 202 Accepted karena pengiriman email sifatnya async
        return ResponseEntity.accepted().body(body);
    }

    @PostMapping("/login-otp")
    @Audited(action = "LOGIN_OTP")
    public TokenPairResponse loginOtp(@Valid @RequestBody LoginWithOtpRequest r,
                                      HttpServletRequest req){
        log.info("Incoming login otp");
        String ip = Optional.ofNullable(req.getHeader("X-Forwarded-For")).orElseGet(req::getRemoteAddr);
        String ua = Optional.ofNullable(req.getHeader("User-Agent")).orElse("-");

        log.info("Outgoing login otp success");
        return loginWithOtpHandler.execute(
                new LoginWithOtp(r.usernameOrEmail(), r.password(), r.otp()), ip, ua);
    }

    /** Mask sederhana: tampilkan 2 huruf depan sebelum '@' */
    private static String maskEmail(String email){
        if (email == null || !email.contains("@")) return "-";
        int at = email.indexOf('@');
        String name = email.substring(0, at);
        String domain = email.substring(at);
        String visible = name.length() <= 2 ? name.substring(0, Math.max(1, name.length())) : name.substring(0, 2);
        return visible + "*******" + domain;
    }
}

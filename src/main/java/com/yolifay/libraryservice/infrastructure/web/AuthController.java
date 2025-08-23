package com.yolifay.libraryservice.infrastructure.web;

import com.yolifay.libraryservice.application.dto.auth.LoginRequest;
import com.yolifay.libraryservice.application.dto.auth.RegisterRequest;
import com.yolifay.libraryservice.application.dto.auth.TokenResponse;
import com.yolifay.libraryservice.domain.service.TokenIssuer;
import com.yolifay.libraryservice.domain.service.TokenStore;
import com.yolifay.libraryservice.domain.usecase.auth.command.LoginUser;
import com.yolifay.libraryservice.domain.usecase.auth.command.RegisterUser;
import com.yolifay.libraryservice.domain.usecase.auth.handler.LoginUserHandler;
import com.yolifay.libraryservice.domain.usecase.auth.handler.RegisterUserHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final RegisterUserHandler registerHandler;
    private final LoginUserHandler loginHandler;
    private final TokenIssuer tokenIssuer;
    private final TokenStore tokenStore;

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
    public TokenResponse login(@Valid @RequestBody LoginRequest lr) {
        log.info("Incoming login user with usernameOrEmail: {}", lr.usernameOrEmail());
        var t = loginHandler.execute(new LoginUser(lr.usernameOrEmail(), lr.password()));

        log.info("Outgoing login user with usernameOrEmail: {}", lr.usernameOrEmail());
        return new TokenResponse(t.value(), t.issuedAt(), t.expiresAt());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader){
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            var dec = tokenIssuer.verify(token);
            tokenStore.revoke(dec.jti());
        }
        return ResponseEntity.noContent().build();
    }
}

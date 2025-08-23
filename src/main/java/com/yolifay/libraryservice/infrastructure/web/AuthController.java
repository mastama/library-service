package com.yolifay.libraryservice.infrastructure.web;

import com.yolifay.libraryservice.application.dto.auth.LoginRequest;
import com.yolifay.libraryservice.application.dto.auth.RegisterRequest;
import com.yolifay.libraryservice.application.dto.auth.TokenResponse;
import com.yolifay.libraryservice.domain.usecase.auth.command.LoginUser;
import com.yolifay.libraryservice.domain.usecase.auth.command.RegisterUser;
import com.yolifay.libraryservice.domain.usecase.auth.handler.LoginUserHandler;
import com.yolifay.libraryservice.domain.usecase.auth.handler.RegisterUserHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final RegisterUserHandler registerHandler;
    private final LoginUserHandler loginHandler;

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
    public TokenResponse login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Incoming login user with usernameOrEmail: {}", loginRequest.usernameOrEmail());
        String token = loginHandler.executeLoginUser(new LoginUser(
                loginRequest.usernameOrEmail(),
                loginRequest.password()
        ));
        log.info("Outgoing login user with usernameOrEmail: {}", loginRequest.usernameOrEmail());
        return new TokenResponse(token);
    }
}

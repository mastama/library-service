package com.yolifay.libraryservice.domain.usecase.auth.handler;

import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.service.Clock;
import com.yolifay.libraryservice.domain.service.PasswordHasher;
import com.yolifay.libraryservice.domain.usecase.auth.command.RegisterUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegisterUserHandler {
    private final UserRepositoryPort users;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public Long executeRegisterUser(RegisterUser regUser) {
        log.info("[REGISTER] start username={}, email={}", regUser.username(), regUser.email());

        if (users.existsByUsername(regUser.username().toLowerCase())) {
            log.warn("[REGISTER] username exists: {}", regUser.username());
            throw new IllegalArgumentException("Username already exists");
        }
        if (users.existsByEmail(regUser.email().toLowerCase())) {
            log.warn("[REGISTER] email exists: {}", regUser.email());
            throw new IllegalArgumentException("Email already exists");
        }

        var user = User.newUser(
                regUser.fullName(),
                regUser.username().toLowerCase(),
                regUser.email().toLowerCase(),
                passwordHasher.hash(regUser.password()),
                clock.now(),
                regUser.roleOrDefault()   // <-- aman: default VIEWER kalau null
        );

        var saved = users.save(user);
        log.info("[REGISTER] success userId={}, role={}", saved.getId(), saved.getRole());
        return saved.getId();
    }
}

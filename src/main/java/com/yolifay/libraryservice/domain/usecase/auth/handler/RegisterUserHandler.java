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
        log.info("Start register user: {}", regUser.username());
        if (users.existsByUsername(regUser.username().toLowerCase())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (users.existsByEmail(regUser.email().toLowerCase())) {
            throw new IllegalArgumentException("Email already exists");
        }
        var user = User.newUser(
                regUser.fullName(),
                regUser.username().toLowerCase(),
                regUser.email().toLowerCase(),
                passwordHasher.hash(regUser.password()),
                clock.now()
        );
        log.info("End register user: {}", regUser.username());
        return users.save(user).getId();
    }
}

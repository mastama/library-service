package com.yolifay.libraryservice.domain.usecase.auth.handler;

import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.service.PasswordHasher;
import com.yolifay.libraryservice.domain.service.TokenIssuer;
import com.yolifay.libraryservice.domain.usecase.auth.command.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginUserHandler {
    private final UserRepositoryPort users;
    private final PasswordHasher hasher;
    private final TokenIssuer tokenIssuer;

    public String executeLoginUser(LoginUser c) {
        User u = users.findByUsernameOrEmail(c.usernameOrEmail().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!hasher.matches(c.password(), u.getPasswordHash()))
            throw new IllegalArgumentException("Invalid credentials");

        return tokenIssuer.issue(u.getId(), u.getUsername(), u.getEmail(), u.getFullName());
    }
}

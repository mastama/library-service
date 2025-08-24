package com.yolifay.libraryservice.domain.usecase.auth.handler;

import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.service.Clock;
import com.yolifay.libraryservice.domain.service.PasswordHasher;
import com.yolifay.libraryservice.domain.usecase.auth.command.RegisterUser;
import com.yolifay.libraryservice.infrastructure.ratelimit.RateLimitGuard;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

import static com.yolifay.libraryservice.infrastructure.web.handler.RequestInfoHandler.currentUserId;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegisterUserHandler {
    private final UserRepositoryPort users;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    private final RateLimitGuard rl;
    private final HttpServletRequest httpServletRequest;

    @CacheEvict(cacheNames = {"users.byId","users.list"}, allEntries = true)
    public Long executeRegisterUser(RegisterUser regUser) {
        log.info("[REGISTER] start username={}, email={}", regUser.username(), regUser.email());

        Long adminId = currentUserId();
        // batasi per admin, identity = username yang mau dibuat
        rl.check("register", httpServletRequest, adminId, regUser.username());

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

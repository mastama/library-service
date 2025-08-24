package com.yolifay.libraryservice.domain.usecase.user.handler;

import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.usecase.user.command.GetUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetUserHandler {
    private final UserRepositoryPort userRepo;

    @Cacheable(cacheNames = "users.byId", key = "#c.id()")
    public User executeGetUser(GetUser c) {
        log.info("[USER] get id={}", c.id());
        return userRepo.findById(c.id()).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}

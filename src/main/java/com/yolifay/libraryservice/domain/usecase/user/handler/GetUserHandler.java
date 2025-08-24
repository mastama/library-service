package com.yolifay.libraryservice.domain.usecase.user.handler;

import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.usecase.user.command.GetUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetUserHandler {
    private final UserRepositoryPort userRepo;
    public User executeGetUser(GetUser c){ return userRepo.findById(c.id()).orElseThrow(() -> new IllegalArgumentException("User not found")); }
}

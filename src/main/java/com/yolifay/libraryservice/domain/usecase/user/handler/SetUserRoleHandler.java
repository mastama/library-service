package com.yolifay.libraryservice.domain.usecase.user.handler;

import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.usecase.user.command.SetUserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SetUserRoleHandler {
    private final UserRepositoryPort userRepo;

    public User executeSetUserRole(SetUserRole c){
        var u = userRepo.findById(c.id()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        var updated = User.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .username(u.getUsername())
                .email(u.getEmail())
                .passwordHash(u.getPasswordHash())
                .createdAt(u.getCreatedAt())
                .role(c.role())
                .build();
        var saved = userRepo.save(updated);
        log.info("[USER] role changed id={} role={}", saved.getId(), saved.getRole());
        return saved;
    }
}

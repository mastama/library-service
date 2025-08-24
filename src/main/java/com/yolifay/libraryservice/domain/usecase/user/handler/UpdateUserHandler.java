package com.yolifay.libraryservice.domain.usecase.user.handler;

import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.service.Clock;
import com.yolifay.libraryservice.domain.service.PasswordHasher;
import com.yolifay.libraryservice.domain.usecase.user.command.UpdateUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateUserHandler {
    private final UserRepositoryPort userRepo;
    private final PasswordHasher hasher;
    private final Clock clock;

    @CacheEvict(cacheNames = {"users.byId","users.list"}, allEntries = true)
    public User executeUpdateUser(UpdateUser c){
        var u = userRepo.findById(c.id()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        String newFull = c.fullName() != null ? c.fullName() : u.getFullName();
        String newEmail = c.email() != null ? c.email().toLowerCase() : u.getEmail();
        String newHash  = (c.newPassword()!=null && !c.newPassword().isBlank())
                ? hasher.hash(c.newPassword())
                : u.getPasswordHash();

        var updated = User.builder()
                .id(u.getId())
                .fullName(newFull)
                .username(u.getUsername())
                .email(newEmail)
                .passwordHash(newHash)
                .createdAt(u.getCreatedAt())
                .role(u.getRole())
                .build();

        var saved = userRepo.save(updated);
        log.info("[USER] updated id={} email={}", saved.getId(), saved.getEmail());
        return saved;
    }
}

package com.yolifay.libraryservice.application.dto.user;

import com.yolifay.libraryservice.domain.model.Role;

import java.time.Instant;

public record UserResponse(
        Long id, String fullName, String username, String email,
        Role role, Instant createdAt
) {
}

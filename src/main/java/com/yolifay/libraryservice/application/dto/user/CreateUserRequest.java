package com.yolifay.libraryservice.application.dto.user;

import com.yolifay.libraryservice.domain.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank String fullName,
        @NotBlank String username,
        @Email @NotBlank String email,
        @NotBlank String password,
        Role role // optional; jika null -> VIEWER
) {
}

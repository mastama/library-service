package com.yolifay.libraryservice.application.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String usernameOrEmail,
        @NotBlank String password
) {
}

package com.yolifay.libraryservice.application.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginWithOtpRequest(
        @NotBlank String usernameOrEmail,
        @NotBlank String password,
        @NotBlank String otp
) {
}

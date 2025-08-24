package com.yolifay.libraryservice.application.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RequestOtpRequest(
        @NotBlank String usernameOrEmail
) {
}

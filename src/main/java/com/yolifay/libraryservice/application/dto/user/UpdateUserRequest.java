package com.yolifay.libraryservice.application.dto.user;

import jakarta.validation.constraints.Email;

public record UpdateUserRequest(
        String fullName,
        @Email String email,
        String password // optional; jika null/blank -> tidak diubah
) {
}

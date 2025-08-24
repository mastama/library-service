package com.yolifay.libraryservice.domain.usecase.user.command;

public record UpdateUser(
        Long id,
        String fullName,
        String email,
        String newPassword
) {
}

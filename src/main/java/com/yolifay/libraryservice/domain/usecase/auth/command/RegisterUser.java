package com.yolifay.libraryservice.domain.usecase.auth.command;

import com.yolifay.libraryservice.domain.model.Role;

public record RegisterUser(
        String fullName,
        String username,
        String email,
        String password,
        Role role
) {
    public Role roleOrDefault() {
        return role == null ? Role.VIEWER : role;
    }
}

package com.yolifay.libraryservice.domain.usecase.user.command;

import com.yolifay.libraryservice.domain.model.Role;

public record SetUserRole(
        Long id,
        Role role
) {
}

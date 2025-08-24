package com.yolifay.libraryservice.application.dto.user;

import com.yolifay.libraryservice.domain.model.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull Role role
        ) {
}

package com.yolifay.libraryservice.domain.usecase.user.command;

public record ListUsers(
        int page,
        int size
) {
}

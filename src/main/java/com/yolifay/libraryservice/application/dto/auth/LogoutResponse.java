package com.yolifay.libraryservice.application.dto.auth;

public record LogoutResponse(
        boolean accessRevoked,
        boolean refreshRevoked
) {
}

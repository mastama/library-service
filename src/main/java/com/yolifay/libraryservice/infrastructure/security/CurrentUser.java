package com.yolifay.libraryservice.infrastructure.security;

import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() {
    }

    public static Long id() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        Object details = auth.getDetails();
        return (details instanceof Long l) ? l : null; // di JwtAuthFilter set details = userId
    }
}

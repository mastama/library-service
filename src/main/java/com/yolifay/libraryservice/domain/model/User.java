package com.yolifay.libraryservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
@Builder
public class User {
    private final Long id;
    private final String fullName;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final Instant createdAt;

    public static User newUser(String fullName, String username, String email, String passwordHash, Instant now) {
        return new User(null, fullName, username.toLowerCase(), email.toLowerCase(), passwordHash, now);
    }
}

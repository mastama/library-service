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
    private final Role role;

    public User(long id, String testuser, String test, Long id1, String fullName, String username, String email, String passwordHash, Instant createdAt, Role role) {
        this.id = id1;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
        this.role = role;
    }

    public static User newUser(String fullName, String username, String email,
                               String passwordHash, Instant now, Role role) {
        return new User(null, fullName, username.toLowerCase(), email.toLowerCase(),
                passwordHash, now, role == null ? Role.VIEWER : role);
    }
}

package com.yolifay.libraryservice.domain.service;

public interface PasswordHasher {
    String hash(String raw);
    boolean matches(String raw, String hashed);
}

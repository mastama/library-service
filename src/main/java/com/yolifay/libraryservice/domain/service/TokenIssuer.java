package com.yolifay.libraryservice.domain.service;

public interface TokenIssuer {
    String issue(Long userId, String username, String email, String fullName); // create JWT
    DecodedToken verify(String token); // parse + verify

    record DecodedToken(Long userId, String username, String email) {}
}

package com.yolifay.libraryservice.domain.service;

import java.time.Duration;

public interface OtpStore {
    void put(String key, String code, Duration ttl);
    String get(String key); // null if not found or expired
    void remove(String key);
}

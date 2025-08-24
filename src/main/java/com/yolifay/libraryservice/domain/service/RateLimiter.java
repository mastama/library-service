package com.yolifay.libraryservice.domain.service;

import java.time.Duration;

public interface RateLimiter {
    /** true jika diizinkan; false jika melebihi limit */
    boolean allow(String key, int limit, Duration window);
    /** sisa waktu (detik) dari window saat ini, -1 jika tidak tersedia */
    long retryAfterSeconds(String key, Duration window);
}

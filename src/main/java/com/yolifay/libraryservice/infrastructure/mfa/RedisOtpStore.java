package com.yolifay.libraryservice.infrastructure.mfa;

import com.yolifay.libraryservice.domain.service.OtpStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisOtpStore implements OtpStore {

    private final StringRedisTemplate redis;

    @Value("${app.auth.redis-otp-prefix:auth:otp:}")
    String prefix;

    private String key(String k) {
        return prefix + k;
    }

    @Override public void put(String k, String code, Duration ttl) {
        redis.opsForValue().set(key(k), code, ttl);
    }

    @Override public String get(String k) {
        return redis.opsForValue().get(key(k));
    }

    @Override public void remove(String k) {
        redis.delete(key(k));
    }
}

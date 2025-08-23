package com.yolifay.libraryservice.infrastructure.cache;

import com.yolifay.libraryservice.domain.service.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {
    private final StringRedisTemplate redis;

    @Value("${app.auth.redis-refresh-prefix:auth:refresh:}")
    private String prefix;

    private static final SecureRandom RNG = new SecureRandom();

    private String key(String token){ return prefix + token; }

    private static String randomToken(){
        byte[] buf = new byte[32]; // 256-bit
        RNG.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    @Override
    public String issue(Long userId, Duration ttl) {
        String token = randomToken();
        redis.opsForValue().set(key(token), String.valueOf(userId), ttl);
        return token;
    }

    @Override
    public Long consume(String token) {
        String k = key(token);
        String val = redis.opsForValue().get(k);
        if (val != null) redis.delete(k); // non-atomic ok untuk use case dev
        return (val == null) ? null : Long.valueOf(val);
    }

    @Override
    public void revoke(String token) {
        redis.delete(key(token));
    }
}

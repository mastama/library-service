package com.yolifay.libraryservice.infrastructure.cache;

import com.yolifay.libraryservice.domain.service.TokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisTokenStore implements TokenStore {

    private final StringRedisTemplate redis;

    @Value("${app.auth.redis-whitelist-prefix:auth:whitelist:}")
    private String prefix;

    private String key(String jti) { return prefix + jti; }

    @Override
    public void whitelist(String jti, Long userId, Duration ttl) {
        redis.opsForValue().set(key(jti), String.valueOf(userId), ttl);
    }

    @Override
    public boolean isWhitelisted(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(key(jti)));
    }

    @Override
    public void revoke(String jti) {
        redis.delete(key(jti));
    }
}

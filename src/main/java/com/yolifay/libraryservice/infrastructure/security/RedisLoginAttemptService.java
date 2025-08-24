package com.yolifay.libraryservice.infrastructure.security;

import com.yolifay.libraryservice.domain.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisLoginAttemptService implements LoginAttemptService {

    private final StringRedisTemplate redis;

    @Value("${app.auth.redis-attempts-prefix:auth:attempts:}") String attemptsPrefix;
    @Value("${app.auth.redis-block-prefix:auth:block:}") String blockPrefix;

    @Value("${login.max-failed:5}")
    int maxFailed;

    @Value("${login.window:10m}")
    Duration window;

    @Value("${login.lock-duration:30m}")
    Duration lockDuration;

    private String aKey(Long id) {
        return attemptsPrefix + id;
    }
    private String bKey(Long id) {
        return blockPrefix + id;
    }

    @Override
    public int onFailure(Long userId) {
        String k = aKey(userId);
        Long v = redis.opsForValue().increment(k);
        redis.getExpire(k);
        if (Boolean.TRUE.equals(redis.getExpire(k) < 0))
            redis.expire(k, window); // set TTL pertama kali
        int count = v == null ? 0 : v.intValue();
        if (count >= maxFailed) {
            redis.opsForValue().set(bKey(userId), "1", lockDuration);
        }
        return count;
    }

    @Override
    public void onSuccess(Long userId) {
        redis.delete(aKey(userId)); // reset counter
        redis.delete(bKey(userId)); // un-block jika ada
    }

    @Override
    public boolean isBlocked(Long userId) { return Boolean.TRUE.equals(redis.hasKey(bKey(userId))); }

    @Override
    public long blockSecondsLeft(Long userId) {
        Long sec = redis.getExpire(bKey(userId));
        return sec == null ? 0 : sec;
    }
}

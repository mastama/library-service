package com.yolifay.libraryservice.infrastructure.ratelimit;

import com.yolifay.libraryservice.domain.service.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component("redisRateLimiter")
@RequiredArgsConstructor
public class RedisRateLimiter implements RateLimiter {

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean allow(String bucketKey, int limit, Duration window) {
        // key memakai window id (tumbling window) agar reset otomatis
        long winSec = Math.max(1, window.toSeconds());
        long nowSec = System.currentTimeMillis() / 1000;
        long windowId = nowSec / winSec;
        String key = "rl:" + bucketKey + ":" + windowId;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, window);
        }
        boolean allowed = count != null && count <= limit;
        if (!allowed) {
            log.warn("[RATE-LIMIT] BLOCK key={} count={} limit={} window={}s", bucketKey, count, limit, winSec);
        } else if (log.isDebugEnabled()) {
            log.debug("[RATE-LIMIT] PASS key={} count={} limit={} window={}s", bucketKey, count, limit, winSec);
        }
        return allowed;
    }

    @Override
    public long retryAfterSeconds(String bucketKey, Duration window) {
        long winSec = Math.max(1, window.toSeconds());
        long nowSec = System.currentTimeMillis() / 1000;
        long windowId = nowSec / winSec;
        String key = "rl:" + bucketKey + ":" + windowId;
        Long ttl = redisTemplate.getExpire(key);
        return Math.max(1, ttl);
    }
}

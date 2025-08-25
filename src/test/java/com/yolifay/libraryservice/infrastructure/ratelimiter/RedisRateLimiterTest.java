package com.yolifay.libraryservice.infrastructure.ratelimiter;

import com.yolifay.libraryservice.infrastructure.ratelimit.RedisRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisRateLimiterTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> ops = mock(ValueOperations.class);

    private RedisRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new RedisRateLimiter(redis);
        when(redis.opsForValue()).thenReturn(ops);
    }

    private static void assertKeyFormat(String key, String bucket) {
        assertTrue(key.startsWith("rl:" + bucket + ":"), "key must start with rl:" + bucket + ":");
        String[] parts = key.split(":");
        assertEquals(3, parts.length);
        assertTrue(parts[2].matches("\\d+"), "windowId must be digits: " + parts[2]);
    }

    // ===== allow(): hit pertama -> set expire & allowed = true =====
//    @Test
//    void allow_firstHit_setsExpire_andAllows() {
//        when(ops.increment(anyString())).thenReturn(1L);
//
//        boolean allowed = limiter.allow("bucketA", 3, Duration.ofSeconds(10));
//
//        assertTrue(allowed);
//
//        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
//        ArgumentCaptor<Duration> durCap = ArgumentCaptor.forClass(Duration.class);
//        verify(ops).increment(keyCap.capture());
//        verify(redis).expire(keyCap.getValue(), durCap.capture());
//
//        String key = keyCap.getValue();
//        assertKeyFormat(key, "bucketA");
//        assertEquals(Duration.ofSeconds(10), durCap.getValue());
//
//        // tidak ada pemanggilan lain yang tak perlu
//        verifyNoMoreInteractions(redis);
//    }

    // ===== allow(): hit dalam limit (>1) -> allowed = true, tidak expire =====
    @Test
    void allow_withinLimit_noExpire_andAllows() {
        when(ops.increment(anyString())).thenReturn(2L);

        boolean allowed = limiter.allow("bucketB", 5, Duration.ofSeconds(8));

        assertTrue(allowed);
        verify(ops).increment(anyString());
        verify(redis, never()).expire(anyString(), any());
    }

    // ===== allow(): melewati limit -> blocked (false), tidak expire =====
    @Test
    void allow_overLimit_blocksFalse_andNoExpire() {
        when(ops.increment(anyString())).thenReturn(6L); // limit 5

        boolean allowed = limiter.allow("bucketC", 5, Duration.ofSeconds(12));

        assertFalse(allowed);
        verify(ops).increment(anyString());
        verify(redis, never()).expire(anyString(), any());
    }

    // ===== allow(): count null -> blocked (false) =====
    @Test
    void allow_nullCount_blocksFalse() {
        when(ops.increment(anyString())).thenReturn(null);

        boolean allowed = limiter.allow("bucketD", 1, Duration.ofSeconds(3));

        assertFalse(allowed);
        verify(ops).increment(anyString());
        verify(redis, never()).expire(anyString(), any());
    }

    // ===== allow(): window ZERO -> winSec dipaksa 1; tetap berfungsi =====
    @Test
    void allow_zeroWindow_usesWinSec1_andWorks() {
        when(ops.increment(anyString())).thenReturn(1L);

        boolean allowed = limiter.allow("bucketZ", 2, Duration.ZERO);

        assertTrue(allowed);
        // capture key hanya untuk memastikan format benar
        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        verify(ops).increment(keyCap.capture());
        assertKeyFormat(keyCap.getValue(), "bucketZ");
        verify(redis).expire(eq(keyCap.getValue()), eq(Duration.ZERO)); // expire dipanggil dengan Duration.ZERO sesuai kode
    }

    // ===== retryAfterSeconds(): TTL normal (>1) =====
    @Test
    void retryAfterSeconds_returnsTtlWhenPositive() {
        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        when(redis.getExpire(anyString())).thenReturn(12L);

        long ttl = limiter.retryAfterSeconds("bucketX", Duration.ofSeconds(10));

        assertEquals(12L, ttl);
        verify(redis).getExpire(keyCap.capture());
        assertKeyFormat(keyCap.getValue(), "bucketX");
    }

    // ===== retryAfterSeconds(): TTL 0 -> dikembalikan minimal 1 =====
    @Test
    void retryAfterSeconds_zeroTtl_returnsAtLeastOne() {
        when(redis.getExpire(anyString())).thenReturn(0L);

        long ttl = limiter.retryAfterSeconds("bucketY", Duration.ofSeconds(7));

        assertEquals(1L, ttl); // Math.max(1, 0) = 1
        verify(redis).getExpire(anyString());
    }

    // ===== retryAfterSeconds(): TTL null -> NPE (negatif) =====
    @Test
    void retryAfterSeconds_nullTtl_throwsNpe_dueToUnboxing() {
        when(redis.getExpire(anyString())).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> limiter.retryAfterSeconds("bucketN", Duration.ofSeconds(5)));
    }
}

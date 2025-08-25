package com.yolifay.libraryservice.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Field;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisLoginAttemptServiceTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> ops = mock(ValueOperations.class);

    private RedisLoginAttemptService svc;

    @BeforeEach
    void setUp() throws Exception {
        svc = new RedisLoginAttemptService(redis);
        when(redis.opsForValue()).thenReturn(ops);

        // set @Value fields via reflection
        setField("attemptsPrefix", "auth:attempts:");
        setField("blockPrefix", "auth:block:");
        setField("maxFailed", 5);
        setField("window", Duration.ofMinutes(10));
        setField("lockDuration", Duration.ofMinutes(30));
    }

    private void setField(String name, Object value) throws Exception {
        Field f = RedisLoginAttemptService.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(svc, value);
    }

    // ===== onFailure: hit pertama -> TTL < 0 => expire dipanggil, tidak blok =====
    @Test
    void onFailure_firstHit_setsExpire_andReturnsCount1() {
        Long userId = 7L;
        String aKey = "auth:attempts:" + userId;

        when(ops.increment(aKey)).thenReturn(1L);
        // getExpire dipanggil 2x di method; keduanya negatif agar masuk cabang expire
        when(redis.getExpire(aKey)).thenReturn(-1L, -1L);

        int count = svc.onFailure(userId);

        assertEquals(1, count);
        verify(ops).increment(aKey);
        verify(redis, times(2)).getExpire(aKey);
        verify(redis).expire(aKey, Duration.ofMinutes(10));
        // tidak set block karena count < maxFailed
        verify(ops, never()).set(startsWith("auth:block:"), anyString(), any());
    }

    // ===== onFailure: mencapai batas -> set block, tidak expire (TTL >= 0) =====
    @Test
    void onFailure_reachMax_setsBlock_andNoExpire() {
        Long userId = 5L;
        String aKey = "auth:attempts:" + userId;
        String bKey = "auth:block:" + userId;

        when(ops.increment(aKey)).thenReturn(5L); // == maxFailed
        when(redis.getExpire(aKey)).thenReturn(120L, 120L); // ttl positif → tidak expire

        int count = svc.onFailure(userId);

        assertEquals(5, count);
        verify(ops).increment(aKey);
        verify(redis, times(2)).getExpire(aKey);
        verify(redis, never()).expire(anyString(), any());
        verify(ops).set(bKey, "1", Duration.ofMinutes(30));
    }

    // ===== onFailure: increment null -> count 0, tidak blok =====
    @Test
    void onFailure_incrementNull_returnsZero_andNoBlock() {
        Long userId = 9L;
        String aKey = "auth:attempts:" + userId;

        when(ops.increment(aKey)).thenReturn(null);
        when(redis.getExpire(aKey)).thenReturn(60L, 60L);

        int count = svc.onFailure(userId);

        assertEquals(0, count);
        verify(ops).increment(aKey);
        verify(redis, times(2)).getExpire(aKey);
        verify(redis, never()).expire(anyString(), any());
        verify(ops, never()).set(startsWith("auth:block:"), anyString(), any());
    }

    // ===== onFailure: TTL null -> NPE karena unboxing pada '< 0' =====
    @Test
    void onFailure_ttlNull_throwsNpe() {
        Long userId = 11L;
        String aKey = "auth:attempts:" + userId;

        when(ops.increment(aKey)).thenReturn(1L);
        when(redis.getExpire(aKey)).thenReturn(null, null); // panggilan kedua memicu unboxing null

        assertThrows(NullPointerException.class, () -> svc.onFailure(userId));

        verify(ops).increment(aKey);
        verify(redis, times(2)).getExpire(aKey);
        verify(redis, never()).expire(anyString(), any());
        verify(ops, never()).set(anyString(), anyString(), any());
    }

    // ===== onSuccess: hapus kedua key =====
    @Test
    void onSuccess_deletesAttemptAndBlockKeys() {
        Long userId = 13L;

        svc.onSuccess(userId);

        verify(redis).delete("auth:attempts:" + userId);
        verify(redis).delete("auth:block:" + userId);
    }

    // ===== isBlocked: hasKey true -> true =====
    @Test
    void isBlocked_trueWhenKeyExists() {
        Long userId = 15L;
        when(redis.hasKey("auth:block:" + userId)).thenReturn(true);

        assertTrue(svc.isBlocked(userId));
    }

    // ===== isBlocked: hasKey null -> false =====
    @Test
    void isBlocked_falseWhenHasKeyNull() {
        Long userId = 16L;
        when(redis.hasKey("auth:block:" + userId)).thenReturn(null);

        assertFalse(svc.isBlocked(userId));
    }

    // ===== blockSecondsLeft: ttl normal =====
    @Test
    void blockSecondsLeft_returnsTtl() {
        Long userId = 20L;
        when(redis.getExpire("auth:block:" + userId)).thenReturn(77L);

        assertEquals(77L, svc.blockSecondsLeft(userId));
    }

    // ===== blockSecondsLeft: ttl null -> 0 =====
    @Test
    void blockSecondsLeft_nullTtl_returnsZero() {
        Long userId = 21L;
        when(redis.getExpire("auth:block:" + userId)).thenReturn(null);

        assertEquals(0L, svc.blockSecondsLeft(userId));
    }
}


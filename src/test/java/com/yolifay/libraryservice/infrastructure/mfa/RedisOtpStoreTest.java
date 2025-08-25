package com.yolifay.libraryservice.infrastructure.mfa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisOtpStoreTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private RedisOtpStore otpStore;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        otpStore.prefix = "auth:otp:"; // inject value @Value secara manual
        when(redis.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void put_success_shouldStoreValueWithTTL() {
        // Arrange
        String key = "user123";
        String code = "987654";
        Duration ttl = Duration.ofMinutes(5);

        // Act
        otpStore.put(key, code, ttl);

        // Assert
        verify(valueOps).set("auth:otp:" + key, code, ttl);
    }

    @Test
    void get_success_shouldReturnStoredValue() {
        // Arrange
        String key = "user123";
        String expectedCode = "111222";
        when(valueOps.get("auth:otp:" + key)).thenReturn(expectedCode);

        // Act
        String result = otpStore.get(key);

        // Assert
        assertEquals(expectedCode, result);
        verify(valueOps).get("auth:otp:" + key);
    }

    @Test
    void get_shouldReturnNullWhenNotExists() {
        // Arrange
        String key = "user999";
        when(valueOps.get("auth:otp:" + key)).thenReturn(null);

        // Act
        String result = otpStore.get(key);

        // Assert
        assertNull(result);
        verify(valueOps).get("auth:otp:" + key);
    }

    @Test
    void remove_success_shouldDeleteKey() {
        // Arrange
        String key = "user123";

        // Act
        otpStore.remove(key);

        // Assert
        verify(redis).delete("auth:otp:" + key);
    }
}

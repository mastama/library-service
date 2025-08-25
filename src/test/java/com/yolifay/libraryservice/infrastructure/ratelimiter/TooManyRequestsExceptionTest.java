package com.yolifay.libraryservice.infrastructure.ratelimiter;

import com.yolifay.libraryservice.infrastructure.ratelimit.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TooManyRequestsExceptionTest {

    @Test
    void testConstructorAndGetters_Positive() {
        // Given
        String ruleName = "testRule";
        long retryAfter = 10L;

        // When
        TooManyRequestsException ex = new TooManyRequestsException(ruleName, retryAfter);

        // Then
        assertEquals("Too Many Requests", ex.getMessage());
        assertEquals(ruleName, ex.getRuleName());
        assertEquals(retryAfter, ex.getRetryAfterSeconds());
    }

    @Test
    void testConstructorWithNullRuleName_Negative() {
        // Given
        String ruleName = null;
        long retryAfter = 0L;

        // When
        TooManyRequestsException ex = new TooManyRequestsException(ruleName, retryAfter);

        // Then
        assertEquals("Too Many Requests", ex.getMessage());
        assertNull(ex.getRuleName());
        assertEquals(0L, ex.getRetryAfterSeconds());
    }
}


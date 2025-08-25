package com.yolifay.libraryservice.infrastructure.ratelimiter;

import com.yolifay.libraryservice.domain.service.RateLimiter;
import com.yolifay.libraryservice.infrastructure.ratelimit.RateLimitGuard;
import com.yolifay.libraryservice.infrastructure.ratelimit.RateLimitProperties;
import com.yolifay.libraryservice.infrastructure.ratelimit.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitGuardTest {

    private final RateLimiter limiter = mock(RateLimiter.class);
    private final RateLimitProperties props = mock(RateLimitProperties.class);
    private final HttpServletRequest req = mock(HttpServletRequest.class);

    private RateLimitGuard guard;

    @BeforeEach
    void setUp() {
        guard = new RateLimitGuard(limiter, props);
    }

    private RateLimitProperties.Rule rule(RateLimitProperties.KeyBy keyBy, int limit, Duration window) {
        RateLimitProperties.Rule r = mock(RateLimitProperties.Rule.class);
        when(r.getKeyBy()).thenReturn(keyBy);
        when(r.getLimit()).thenReturn(limit);
        when(r.getWindow()).thenReturn(window);
        return r;
    }

    /** helper: set rules map + enable flag */
    private void setRules(Map<String, RateLimitProperties.Rule> rules, boolean enabled) {
        when(props.getRules()).thenReturn(rules);
        when(props.isEnabled()).thenReturn(enabled);
    }

    // ============ Early return paths ============

    @Test
    void check_propsDisabled_returnsEarly_withoutCallingLimiter() {
        setRules(new HashMap<>(), false); // disabled
        guard.check("login", req, 1L, "id");
        verifyNoInteractions(limiter);
    }

    @Test
    void check_ruleMissing_returnsEarly_withoutCallingLimiter() {
        setRules(new HashMap<>(), true); // enabled tapi rule tidak ada
        guard.check("unknown", req, 1L, "id");
        verifyNoInteractions(limiter);
    }

    // ============ KeyBy.IP dengan X-Forwarded-For ============

    @Test
    void check_keyByIp_usesFirstXffToken_andPassAllow() {
        Map<String, RateLimitProperties.Rule> rules = new HashMap<>();
        rules.put("login", rule(RateLimitProperties.KeyBy.IP, 3, Duration.ofSeconds(5)));
        setRules(rules, true);

        when(req.getHeader("X-Forwarded-For")).thenReturn(" 203.0.113.5 , 10.0.0.1 ");
        // allow
        when(limiter.allow("ip:203.0.113.5", 3, Duration.ofSeconds(5))).thenReturn(true);

        assertDoesNotThrow(() -> guard.check("login", req, 42L, null));

        verify(limiter).allow("ip:203.0.113.5", 3, Duration.ofSeconds(5));
        verify(limiter, never()).retryAfterSeconds(anyString(), any());
    }

    // ============ KeyBy.USER dengan identity di-trim+lower dan userId null (anon) ============
    @Test
    void check_keyByUser_usesAnonWhenUserIdNull_andNormalizesIdentity() {
        Map<String, RateLimitProperties.Rule> rules = new HashMap<>();
        rules.put("register", rule(RateLimitProperties.KeyBy.USER, 10, Duration.ofMinutes(1)));
        setRules(rules, true);

        when(req.getHeader("X-Forwarded-For")).thenReturn("  "); // kosong → fallback remoteAddr (tak dipakai di USER)
        when(req.getRemoteAddr()).thenReturn("198.51.100.7");
        when(limiter.allow("u:anon:alice@example.test", 10, Duration.ofMinutes(1))).thenReturn(true);

        // identity di-trim + lower-case
        assertDoesNotThrow(() -> guard.check("register", req, null, "  Alice@Example.TEST  "));

        verify(limiter).allow("u:anon:alice@example.test", 10, Duration.ofMinutes(1));
        verify(limiter, never()).retryAfterSeconds(anyString(), any());
    }

    // ============ KeyBy.IP_USER dengan identity blank (diabaikan) dan fallback remoteAddr ============
    @Test
    void check_keyByIpUser_buildsCompositeKey_andPassAllow_whenIdentityBlank() {
        Map<String, RateLimitProperties.Rule> rules = new HashMap<>();
        rules.put("otp", rule(RateLimitProperties.KeyBy.IP_USER, 2, Duration.ofSeconds(30)));
        setRules(rules, true);

        when(req.getHeader("X-Forwarded-For")).thenReturn(null); // fallback remoteAddr
        when(req.getRemoteAddr()).thenReturn("10.0.0.2");

        // identity blank -> tidak ditambahkan
        when(limiter.allow("ip:10.0.0.2:u:55", 2, Duration.ofSeconds(30))).thenReturn(true);

        assertDoesNotThrow(() -> guard.check("otp", req, 55L, "   "));

        verify(limiter).allow("ip:10.0.0.2:u:55", 2, Duration.ofSeconds(30));
        verify(limiter, never()).retryAfterSeconds(anyString(), any());
    }

    // ============ Blocked: allow == false ⇒ throw TooManyRequestsException ============
    @Test
    void check_blocked_throwsTooManyRequests_withRetryAfter_fromLimiter() {
        Map<String, RateLimitProperties.Rule> rules = new HashMap<>();
        rules.put("login", rule(RateLimitProperties.KeyBy.IP, 1, Duration.ofSeconds(10)));
        setRules(rules, true);

        when(req.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");
        when(limiter.allow("ip:1.2.3.4", 1, Duration.ofSeconds(10))).thenReturn(false);
        when(limiter.retryAfterSeconds("ip:1.2.3.4", Duration.ofSeconds(10))).thenReturn(17L);

        TooManyRequestsException ex =
                assertThrows(TooManyRequestsException.class, () -> guard.check("login", req, 9L, null));

        assertEquals("login", ex.getRuleName());
        assertEquals(17L, ex.getRetryAfterSeconds());

        verify(limiter).allow("ip:1.2.3.4", 1, Duration.ofSeconds(10));
        verify(limiter).retryAfterSeconds("ip:1.2.3.4", Duration.ofSeconds(10));
    }
}


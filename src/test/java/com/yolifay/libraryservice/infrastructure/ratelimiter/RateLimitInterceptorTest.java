package com.yolifay.libraryservice.infrastructure.ratelimiter;

import com.yolifay.libraryservice.domain.service.RateLimiter;
import com.yolifay.libraryservice.infrastructure.ratelimit.RateLimitInterceptor;
import com.yolifay.libraryservice.infrastructure.ratelimit.RateLimitProperties;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitInterceptorTest {

    private RateLimiter limiter;
    private RateLimitProperties props;
    private RateLimitInterceptor interceptor;
    private HttpServletRequest req;
    private HttpServletResponse res;

    @BeforeEach
    void setUp() throws IOException {
        limiter = mock(RateLimiter.class);
        props = new RateLimitProperties();
        interceptor = new RateLimitInterceptor(limiter, props);
        req = mock(HttpServletRequest.class);
        res = mock(HttpServletResponse.class);

        when(req.getRequestURI()).thenReturn("/api/books");
        when(req.getMethod()).thenReturn("GET");

        // Mock OutputStream for response
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ServletOutputStream sos = new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {

            }

            @Override
            public void write(int b) throws IOException {
                bos.write(b);
            }
        };
        when(res.getOutputStream()).thenReturn(sos);
    }

    @Test
    void testPreHandle_disabled_shouldReturnTrue() throws Exception {
        props.setEnabled(false);

        boolean result = interceptor.preHandle(req, res, new Object());

        assertTrue(result);
        verifyNoInteractions(limiter);
    }

    @Test
    void testPreHandle_enabled_noMatchingRule_shouldReturnTrue() throws Exception {
        props.setEnabled(true);
        RateLimitProperties.Rule rule = new RateLimitProperties.Rule();
        rule.setPaths(List.of("/other/**")); // tidak cocok dengan "/api/books"
        props.setRules(Map.of("rule1", rule));

        boolean result = interceptor.preHandle(req, res, new Object());

        assertTrue(result);
        verifyNoInteractions(limiter);
    }

    @Test
    void testPreHandle_enabled_ruleWithMethodNotMatched_shouldReturnTrue() throws Exception {
        props.setEnabled(true);
        RateLimitProperties.Rule rule = new RateLimitProperties.Rule();
        rule.setPaths(List.of("/api/**"));
        rule.setMethod("POST"); // method tidak cocok
        props.setRules(Map.of("rule1", rule));

        boolean result = interceptor.preHandle(req, res, new Object());

        assertTrue(result);
        verifyNoInteractions(limiter);
    }

    @Test
    void testPreHandle_allowedRequest_shouldReturnTrue() throws Exception {
        props.setEnabled(true);
        RateLimitProperties.Rule rule = new RateLimitProperties.Rule();
        rule.setPaths(List.of("/api/**"));
        rule.setLimit(5);
        rule.setWindow(Duration.ofSeconds(60));
        rule.setKeyBy(RateLimitProperties.KeyBy.IP_USER);
        props.setRules(Map.of("rule1", rule));

        when(req.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");
        when(limiter.allow(anyString(), eq(5), eq(Duration.ofSeconds(60)))).thenReturn(true);

        // Mock SecurityContext user ID
        try (MockedStatic<SecurityContextHolder> ctxMock = mockStatic(SecurityContextHolder.class)) {
            SecurityContext context = mock(SecurityContext.class);
            Authentication auth = mock(Authentication.class);
            when(auth.getDetails()).thenReturn("user123");
            when(context.getAuthentication()).thenReturn(auth);
            ctxMock.when(SecurityContextHolder::getContext).thenReturn(context);

            boolean result = interceptor.preHandle(req, res, new Object());

            assertTrue(result);
            verify(limiter).allow(startsWith("ip:"), eq(5), eq(Duration.ofSeconds(60)));
        }
    }

    @Test
    void testPreHandle_blockedRequest_shouldReturnFalseAndWrite429() throws Exception {
        props.setEnabled(true);
        RateLimitProperties.Rule rule = new RateLimitProperties.Rule();
        rule.setPaths(List.of("/api/**"));
        rule.setLimit(1);
        rule.setWindow(Duration.ofSeconds(30));
        rule.setKeyBy(RateLimitProperties.KeyBy.IP);
        props.setRules(Map.of("rule1", rule));

        when(req.getRemoteAddr()).thenReturn("127.0.0.1");
        when(limiter.allow(anyString(), eq(1), eq(Duration.ofSeconds(30)))).thenReturn(false);
        when(limiter.retryAfterSeconds(anyString(), eq(Duration.ofSeconds(30)))).thenReturn(15L);

        boolean result = interceptor.preHandle(req, res, new Object());

        assertFalse(result);
        verify(res).setStatus(429);
        verify(res).setHeader("Retry-After", "15");
        verify(res).setContentType("application/json");
    }

    @Test
    void testCurrentUserId_noAuth_shouldReturnAnon() {
        try (MockedStatic<SecurityContextHolder> ctxMock = mockStatic(SecurityContextHolder.class)) {
            SecurityContext context = mock(SecurityContext.class);
            when(context.getAuthentication()).thenReturn(null);
            ctxMock.when(SecurityContextHolder::getContext).thenReturn(context);

            String result = invokeCurrentUserId();

            assertEquals("anon", result);
        }
    }

    @Test
    void testClientIp_withXForwardedFor_shouldTakeFirst() {
        when(req.getHeader("X-Forwarded-For")).thenReturn("8.8.8.8, 1.1.1.1");
        String result = invokeClientIp();
        assertEquals("8.8.8.8", result);
    }

    @Test
    void testClientIp_noXForwardedFor_shouldUseRemoteAddr() {
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("10.0.0.1");
        String result = invokeClientIp();
        assertEquals("10.0.0.1", result);
    }

    private String invokeClientIp() {
        try {
            var m = RateLimitInterceptor.class.getDeclaredMethod("clientIp", HttpServletRequest.class);
            m.setAccessible(true);
            return (String) m.invoke(interceptor, req);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String invokeCurrentUserId() {
        try {
            var m = RateLimitInterceptor.class.getDeclaredMethod("currentUserId");
            m.setAccessible(true);
            return (String) m.invoke(interceptor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

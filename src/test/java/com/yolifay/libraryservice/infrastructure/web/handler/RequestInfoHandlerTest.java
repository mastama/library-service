package com.yolifay.libraryservice.infrastructure.web.handler;

import com.yolifay.libraryservice.infrastructure.web.handler.RequestInfoHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestInfoHandlerTest {

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    // ---------------- TEST clientIp() ----------------
    @Test
    void clientIp_shouldReturnXForwardedFor_whenPresent() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, proxy");
        String ip = RequestInfoHandler.clientIp(req);
        assertEquals("192.168.1.1", ip);
    }

    @Test
    void clientIp_shouldReturnRemoteAddr_whenXForwardedForBlank() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("   ");
        when(req.getRemoteAddr()).thenReturn("10.0.0.1");
        String ip = RequestInfoHandler.clientIp(req);
        assertEquals("10.0.0.1", ip);
    }

    @Test
    void clientIp_shouldReturnRemoteAddr_whenXForwardedForNull() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");
        String ip = RequestInfoHandler.clientIp(req);
        assertEquals("127.0.0.1", ip);
    }

    // ---------------- TEST currentUserId() ----------------
    @Test
    void currentUserId_shouldReturnNull_whenAuthNull() {
        assertNull(RequestInfoHandler.currentUserId());
    }

    @Test
    void currentUserId_shouldReturnLong_whenDetailsIsLong() {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(123L);
        setAuthContext(auth);
        assertEquals(123L, RequestInfoHandler.currentUserId());
    }

    @Test
    void currentUserId_shouldReturnLong_whenDetailsIsNumber() {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(456);
        setAuthContext(auth);
        assertEquals(456L, RequestInfoHandler.currentUserId());
    }

    @Test
    void currentUserId_shouldReturnLong_whenDetailsIsStringParsable() {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn("789");
        setAuthContext(auth);
        assertEquals(789L, RequestInfoHandler.currentUserId());
    }

    @Test
    void currentUserId_shouldReturnNull_whenDetailsIsStringNotParsable() {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn("abc");
        setAuthContext(auth);
        assertNull(RequestInfoHandler.currentUserId());
    }

    @Test
    void currentUserId_shouldReturnNull_whenDetailsIsOtherObject() {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(new Object());
        setAuthContext(auth);
        assertNull(RequestInfoHandler.currentUserId());
    }

    // ---------------- TEST currentUsername() ----------------
    @Test
    void currentUsername_shouldReturnNull_whenAuthNull() {
        assertNull(RequestInfoHandler.currentUsername());
    }

    @Test
    void currentUsername_shouldReturnName_whenAuthNotNull() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("john");
        setAuthContext(auth);
        assertEquals("john", RequestInfoHandler.currentUsername());
    }

    // ---------------- TEST roles() ----------------
    @Test
    void roles_shouldReturnEmptySet_whenAuthNull() {
        assertTrue(RequestInfoHandler.roles().isEmpty());
    }

    // ---------------- TEST hasRole() ---------------

    private void setAuthContext(Authentication auth) {
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
    }
}

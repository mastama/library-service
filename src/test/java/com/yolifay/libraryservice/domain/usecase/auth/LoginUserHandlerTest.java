package com.yolifay.libraryservice.domain.usecase.auth;

import com.yolifay.libraryservice.application.dto.auth.TokenPairResponse;
import com.yolifay.libraryservice.domain.model.Role;
import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.service.PasswordHasher;
import com.yolifay.libraryservice.domain.service.RefreshTokenStore;
import com.yolifay.libraryservice.domain.service.TokenIssuer;
import com.yolifay.libraryservice.domain.service.TokenStore;
import com.yolifay.libraryservice.domain.usecase.auth.command.LoginUser;
import com.yolifay.libraryservice.domain.usecase.auth.handler.LoginUserHandler;
import com.yolifay.libraryservice.infrastructure.ratelimit.RateLimitGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginUserHandlerTest {

    // ==== mocks ====
    private final UserRepositoryPort users = mock(UserRepositoryPort.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final TokenIssuer tokenIssuer = mock(TokenIssuer.class);
    private final TokenStore accessWhitelist = mock(TokenStore.class);
    private final RefreshTokenStore refreshStore = mock(RefreshTokenStore.class);
    private final RateLimitGuard rl = mock(RateLimitGuard.class);
    private final HttpServletRequest httpReq = mock(HttpServletRequest.class);

    private LoginUserHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        handler = new LoginUserHandler(users, hasher, tokenIssuer, accessWhitelist, refreshStore, rl, httpReq);
        // set @Value refreshExp dengan refleksi agar deterministik di test
        setField(handler, "refreshExp", Duration.ofDays(9));
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private User mockUser(long id, String username, String email, String fullname, String passwordHash, Enum<?> role) {
        User u = mock(User.class);
        when(u.getId()).thenReturn(id);
        when(u.getUsername()).thenReturn(username);
        when(u.getEmail()).thenReturn(email);
        when(u.getFullName()).thenReturn(fullname);
        when(u.getPasswordHash()).thenReturn(passwordHash);
        when(u.getRole()).thenReturn((Role) role);
        return u;
    }

    // ============== POSITIVE ==============

    @Test
    void execute_success_fullFlow_callsAllCollaborators_andBuildsResponse() {
        // input campuran kapital → harus dilower-case saat query repo
        String input = "Alice@Example.TEST";
        LoginUser cmd = new LoginUser(input, "plain");

        // user dari repo
        User user = mockUser(7L, "alice", "alice@example.test", "Alice", "hash", Role.EDITOR);
        when(users.findByUsernameOrEmail("alice@example.test"))
                .thenReturn(Optional.of(user));

        // password cocok
        when(hasher.matches("plain", "hash")).thenReturn(true);

        // TokenIssuer.Token dari PAKET YANG SAMA dengan production
        Instant issued = Instant.parse("2025-01-01T00:00:00Z");
        Instant expires = issued.plusSeconds(7200);
        TokenIssuer.Token access = new TokenIssuer.Token("acc-123", "jti-xyz", issued, expires);

        // gunakan eq(...) untuk semua argumen supaya match tepat
        when(tokenIssuer.issue(
                eq(7L),
                eq("alice"),
                eq("alice@example.test"),
                eq("Alice"),
                eq(Role.EDITOR)
        )).thenReturn(access);

        when(refreshStore.issue(7L, Duration.ofDays(9))).thenReturn("rf-abc");

        Instant before = Instant.now();
        TokenPairResponse out = handler.execute(cmd);
        Instant after = Instant.now();

        verify(rl).check(eq("login"), same(httpReq), isNull(), eq(input));
        verify(users).findByUsernameOrEmail("alice@example.test");
        verify(accessWhitelist).whitelist(eq("jti-xyz"), eq(7L), eq(Duration.ofHours(2)));
        verify(refreshStore).issue(7L, Duration.ofDays(9));

        assertEquals("acc-123", out.accessToken());
        assertEquals(issued, out.accessIssuedAt());
        assertEquals(expires, out.accessExpiresAt());
        assertEquals("rf-abc", out.refreshToken());
        assertTrue(!out.refreshIssuedAt().isBefore(before.plus(Duration.ofDays(9))) &&
                !out.refreshIssuedAt().isAfter(after.plus(Duration.ofDays(9)).plusSeconds(2)));
    }

    // ============== NEGATIVE ==============

    @Test
    void execute_rateLimitThrows_propagates_andStopsFlow() {
        LoginUser cmd = new LoginUser("u", "p");
        doThrow(new RuntimeException("throttled"))
                .when(rl).check(eq("login"), same(httpReq), isNull(), eq("u"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> handler.execute(cmd));
        assertEquals("throttled", ex.getMessage());

        verifyNoInteractions(users, hasher, tokenIssuer, accessWhitelist, refreshStore);
    }

    @Test
    void execute_userNotFound_throwsInvalidCredentials() {
        LoginUser cmd = new LoginUser("U@MAIL", "p");
        when(users.findByUsernameOrEmail("u@mail")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> handler.execute(cmd));
        assertEquals("Invalid credentials", ex.getMessage());

        verify(users).findByUsernameOrEmail("u@mail");
        verifyNoInteractions(hasher, tokenIssuer, accessWhitelist, refreshStore);
    }

    @Test
    void execute_passwordMismatch_throwsInvalidCredentials_andNoTokenIssued() {
        LoginUser cmd = new LoginUser("user@mail", "wrong");
        var user = mockUser(99L, "user", "user@mail", "U", "hash", Role.EDITOR);
        when(users.findByUsernameOrEmail("user@mail")).thenReturn(Optional.of(user));
        when(hasher.matches("wrong", "hash")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> handler.execute(cmd));
        assertEquals("Invalid credentials", ex.getMessage());

        verify(users).findByUsernameOrEmail("user@mail");
        verify(hasher).matches("wrong", "hash");
        verifyNoInteractions(tokenIssuer, accessWhitelist, refreshStore);
    }
}

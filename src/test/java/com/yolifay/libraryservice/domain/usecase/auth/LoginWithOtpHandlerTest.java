package com.yolifay.libraryservice.domain.usecase.auth;

import com.yolifay.libraryservice.application.dto.auth.TokenPairResponse;
import com.yolifay.libraryservice.domain.model.Role;
import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.service.*;

import com.yolifay.libraryservice.domain.usecase.auth.command.LoginWithOtp;
import com.yolifay.libraryservice.domain.usecase.auth.handler.LoginWithOtpHandler;
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

class LoginWithOtpHandlerTest {

    // ====== mocks ======
    private final UserRepositoryPort users = mock(UserRepositoryPort.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final OtpStore otpStore = mock(OtpStore.class);
    private final TokenIssuer tokenIssuer = mock(TokenIssuer.class);
    private final TokenStore accessWhitelist = mock(TokenStore.class);
    private final RefreshTokenStore refreshStore = mock(RefreshTokenStore.class);
    private final LoginAttemptService attempts = mock(LoginAttemptService.class);
    private final RateLimitGuard rl = mock(RateLimitGuard.class);
    private final HttpServletRequest httpReq = mock(HttpServletRequest.class);

    private LoginWithOtpHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        handler = new LoginWithOtpHandler(
                users, hasher, otpStore, tokenIssuer, accessWhitelist, refreshStore, attempts,
                rl, httpReq
        );
        // set @Value refreshExp supaya deterministik
        setField(handler, Duration.ofDays(5));
    }

    private static void setField(Object target, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField("refreshExp");
        f.setAccessible(true);
        f.set(target, value);
    }

    private User mockUser(long id, String username, String email, String fullName, String hash, Role role) {
        User u = mock(User.class);
        when(u.getId()).thenReturn(id);
        when(u.getUsername()).thenReturn(username);
        when(u.getEmail()).thenReturn(email);
        when(u.getFullName()).thenReturn(fullName);
        when(u.getPasswordHash()).thenReturn(hash);
        when(u.getRole()).thenReturn(role);
        return u;
    }

    // ================== POSITIVE ==================

    @Test
    void execute_success_issuesTokens_whitelists_resetsCounters_andRemovesOtp() {
        String input = "Alice@Example.TEST";
        String ip = "1.2.3.4";
        String ua = "UA";
        LoginWithOtp cmd = new LoginWithOtp(input, "plain", "654321");

        // user ditemukan (lower-case dicari ke repo)
        User user = mockUser(7L, "alice", "alice@example.test", "Alice", "hash", Role.EDITOR);
        when(users.findByUsernameOrEmail("alice@example.test")).thenReturn(Optional.of(user));

        // tidak diblok
        when(attempts.isBlocked(7L)).thenReturn(false);

        // password cocok
        when(hasher.matches("plain", "hash")).thenReturn(true);

        // OTP cocok
        when(otpStore.get("login:7")).thenReturn("654321");

        // issue access token
        Instant issued = Instant.parse("2025-01-01T00:00:00Z");
        Instant expires = issued.plusSeconds(3600);
        TokenIssuer.Token access = new TokenIssuer.Token("acc", "jti-1", issued, expires);
        when(tokenIssuer.issue(7L, "alice", "alice@example.test", "Alice", Role.EDITOR))
                .thenReturn(access);

        // issue refresh
        when(refreshStore.issue(7L, Duration.ofDays(5))).thenReturn("rf");

        // act
        Instant before = Instant.now();
        TokenPairResponse out = handler.execute(cmd, ip, ua);
        Instant after = Instant.now();

        // assert rate limit dipanggil
        verify(rl).check(eq("login"), same(httpReq), isNull(), eq(input));

        // lookup lower-case
        verify(users).findByUsernameOrEmail("alice@example.test");

        // OTP dihapus
        verify(otpStore).remove("login:7");

        // reset counter sukses
        verify(attempts).onSuccess(7L);

        // whitelisting dengan TTL benar
        verify(accessWhitelist).whitelist(eq("jti-1"), eq(7L), eq(Duration.ofHours(1)));

        // refresh store issue
        verify(refreshStore).issue(7L, Duration.ofDays(5));

        // response field benar
        assertEquals("acc", out.accessToken());
        assertEquals(issued, out.accessIssuedAt());
        assertEquals(expires, out.accessExpiresAt());
        assertEquals("rf", out.refreshToken());
        assertTrue(!out.refreshIssuedAt().isBefore(before.plus(Duration.ofDays(5))) &&
                !out.refreshIssuedAt().isAfter(after.plus(Duration.ofDays(5)).plusSeconds(2)));
    }

    // ================== NEGATIVE ==================

    @Test
    void execute_rateLimited_throws_andStopsFlow() {
        LoginWithOtp cmd = new LoginWithOtp("user@mail", "p", "1");
        doThrow(new RuntimeException("throttled"))
                .when(rl).check(eq("login"), same(httpReq), isNull(), eq("user@mail"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> handler.execute(cmd, "ip", "ua"));
        assertEquals("throttled", ex.getMessage());

        verifyNoInteractions(users, hasher, otpStore, tokenIssuer, accessWhitelist, refreshStore, attempts);
    }

    @Test
    void execute_userNotFound_throwsInvalidCredentials() {
        LoginWithOtp cmd = new LoginWithOtp("User@Mail", "p", "1");
        when(users.findByUsernameOrEmail("user@mail")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> handler.execute(cmd, "ip", "ua"));
        assertEquals("Invalid credentials", ex.getMessage());

        verify(users).findByUsernameOrEmail("user@mail");
        verifyNoInteractions(hasher, otpStore, tokenIssuer, accessWhitelist, refreshStore, attempts);
    }

    @Test
    void execute_userBlocked_throwsBlockedMessage_andCallsBlockSecondsLeft() {
        LoginWithOtp cmd = new LoginWithOtp("u@mail", "p", "1");
        User user = mockUser(9L, "u", "u@mail", "U", "h", Role.VIEWER);
        when(users.findByUsernameOrEmail("u@mail")).thenReturn(Optional.of(user));

        when(attempts.isBlocked(9L)).thenReturn(true);
        when(attempts.blockSecondsLeft(9L)).thenReturn(120L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> handler.execute(cmd, "ip", "ua"));
        assertEquals("Account is temporarily blocked. Try again later.", ex.getMessage());

        verify(attempts).blockSecondsLeft(9L);
        verifyNoInteractions(hasher, otpStore, tokenIssuer, accessWhitelist, refreshStore);
    }

    @Test
    void execute_passwordMismatch_throwsInvalidCredentials_andIncrementsAttempt() {
        LoginWithOtp cmd = new LoginWithOtp("u@mail", "wrong", "1");
        User user = mockUser(5L, "u", "u@mail", "U", "hash", Role.VIEWER);
        when(users.findByUsernameOrEmail("u@mail")).thenReturn(Optional.of(user));

        when(attempts.isBlocked(5L)).thenReturn(false);
        when(hasher.matches("wrong", "hash")).thenReturn(false);
        when(attempts.onFailure(5L)).thenReturn(2);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> handler.execute(cmd, "ip", "ua"));
        assertEquals("Invalid credentials", ex.getMessage());

        verify(attempts).onFailure(5L);
        verifyNoInteractions(otpStore, tokenIssuer, accessWhitelist, refreshStore);
    }

    @Test
    void execute_badOtp_nullCode_throwsInvalidOtp_andIncrementsAttempt() {
        LoginWithOtp cmd = new LoginWithOtp("u@mail", "p", "111111");
        User user = mockUser(6L, "u", "u@mail", "U", "hash", Role.EDITOR);
        when(users.findByUsernameOrEmail("u@mail")).thenReturn(Optional.of(user));

        when(attempts.isBlocked(6L)).thenReturn(false);
        when(hasher.matches("p", "hash")).thenReturn(true);
        when(otpStore.get("login:6")).thenReturn(null);
        when(attempts.onFailure(6L)).thenReturn(3);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> handler.execute(cmd, "ip", "ua"));
        assertEquals("Invalid OTP", ex.getMessage());

        verify(attempts).onFailure(6L);
        verify(otpStore, never()).remove(anyString());
        verifyNoInteractions(tokenIssuer, accessWhitelist, refreshStore);
    }

    @Test
    void execute_badOtp_mismatch_throwsInvalidOtp_andIncrementsAttempt() {
        LoginWithOtp cmd = new LoginWithOtp("u@mail", "p", "222222");
        User user = mockUser(10L, "u", "u@mail", "U", "hash", Role.EDITOR);
        when(users.findByUsernameOrEmail("u@mail")).thenReturn(Optional.of(user));

        when(attempts.isBlocked(10L)).thenReturn(false);
        when(hasher.matches("p", "hash")).thenReturn(true);
        when(otpStore.get("login:10")).thenReturn("000000");
        when(attempts.onFailure(10L)).thenReturn(4);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> handler.execute(cmd, "ip", "ua"));
        assertEquals("Invalid OTP", ex.getMessage());

        verify(attempts).onFailure(10L);
        verify(otpStore, never()).remove(anyString());
        verifyNoInteractions(tokenIssuer, accessWhitelist, refreshStore);
    }
}


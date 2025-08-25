package com.yolifay.libraryservice.domain.usecase.auth;

import com.yolifay.libraryservice.domain.model.Role;
import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.service.RefreshTokenStore;
import com.yolifay.libraryservice.domain.service.TokenIssuer;
import com.yolifay.libraryservice.domain.service.TokenStore;
import com.yolifay.libraryservice.domain.usecase.auth.command.RefreshAccessToken;
import com.yolifay.libraryservice.domain.usecase.auth.handler.RefreshAccessTokenHandler;
import com.yolifay.libraryservice.infrastructure.ratelimit.RateLimitGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshAccessTokenHandlerTest {

    @Mock
    private RefreshTokenStore refreshStore;
    @Mock
    private UserRepositoryPort users;
    @Mock
    private TokenIssuer tokenIssuer;
    @Mock
    private TokenStore tokenWhitelist;
    @Mock
    private RateLimitGuard rl;
    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private RefreshAccessTokenHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler.refreshDays = 14L; // inject value @Value secara manual
    }

    @Test
    void execute_success_shouldReturnNewTokenPair() {
        // Arrange
        RefreshAccessToken command = new RefreshAccessToken("refresh-token-xyz");
        Long userId = 123L;

        // Mock refreshStore.consume()
        when(refreshStore.consume("refresh-token-xyz")).thenReturn(userId);

        // Mock RateLimitGuard
        doNothing().when(rl).check(eq("refresh"), eq(httpServletRequest), eq(userId), isNull());

        // Mock user repository
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getUsername()).thenReturn("john");
        when(user.getEmail()).thenReturn("john@example.com");
        when(user.getFullName()).thenReturn("John Doe");
        when(user.getRole()).thenReturn(Role.EDITOR);
        when(users.findById(userId)).thenReturn(Optional.of(user));

        // Mock token issuer
        TokenIssuer.Token accessToken = new TokenIssuer.Token(
                "jwt-token", "jti-123",
                Instant.now(), Instant.now().plusSeconds(3600)
        );
        when(tokenIssuer.issue(userId, "john", "john@example.com", "John Doe", Role.EDITOR))
                .thenReturn(accessToken);

        // Mock refreshStore.issue()
        when(refreshStore.issue(eq(userId), any())).thenReturn("new-refresh-token");

        // Act
        RefreshAccessTokenHandler.TokenPair result = handler.execute(command);

        // Assert
        assertNotNull(result);
        assertEquals("jwt-token", result.accessToken().value());
        assertEquals("new-refresh-token", result.refreshToken());

        // Verify calls
        verify(refreshStore).consume("refresh-token-xyz");
        verify(rl).check(eq("refresh"), eq(httpServletRequest), eq(userId), isNull());
        verify(users).findById(userId);
        verify(tokenIssuer).issue(userId, "john", "john@example.com", "John Doe", Role.EDITOR);
        verify(tokenWhitelist).whitelist(eq("jti-123"), eq(userId), any());
        verify(refreshStore).issue(eq(userId), any(Duration.class));
    }

    @Test
    void execute_shouldThrow_whenRefreshTokenInvalid() {
        // Arrange
        RefreshAccessToken command = new RefreshAccessToken("invalid-refresh");
        when(refreshStore.consume("invalid-refresh")).thenReturn(null);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> handler.execute(command));

        assertEquals("Invalid refresh token", ex.getMessage());
        verify(refreshStore).consume("invalid-refresh");
        verify(rl).check(eq("refresh"), eq(httpServletRequest), eq(null), isNull());
        verifyNoInteractions(users, tokenIssuer, tokenWhitelist);
    }

    @Test
    void execute_shouldThrow_whenUserNotFound() {
        // Arrange
        RefreshAccessToken command = new RefreshAccessToken("valid-refresh");
        Long userId = 123L;

        when(refreshStore.consume("valid-refresh")).thenReturn(userId);
        doNothing().when(rl).check(eq("refresh"), eq(httpServletRequest), eq(userId), isNull());
        when(users.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> handler.execute(command));

        assertEquals("User not found", ex.getMessage());
        verify(refreshStore).consume("valid-refresh");
        verify(users).findById(userId);
        verifyNoInteractions(tokenIssuer, tokenWhitelist);
    }
}

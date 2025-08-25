package com.yolifay.libraryservice.domain.usecase.auth;

import com.yolifay.libraryservice.domain.model.Role;
import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.service.Clock;
import com.yolifay.libraryservice.domain.service.PasswordHasher;
import com.yolifay.libraryservice.domain.usecase.auth.command.RegisterUser;
import com.yolifay.libraryservice.domain.usecase.auth.handler.RegisterUserHandler;
import com.yolifay.libraryservice.infrastructure.ratelimit.RateLimitGuard;
import com.yolifay.libraryservice.infrastructure.ratelimit.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegisterUserHandlerTest {

    private UserRepositoryPort userRepository;
    private PasswordHasher passwordHasher;
    private Clock clock;
    private RateLimitGuard rl;
    private HttpServletRequest httpServletRequest;
    private RegisterUserHandler handler;

    @BeforeEach
    void setup() {
        userRepository = mock(UserRepositoryPort.class);
        passwordHasher = mock(PasswordHasher.class);
        clock = mock(Clock.class);
        rl = mock(RateLimitGuard.class);
        httpServletRequest = mock(HttpServletRequest.class);

        handler = new RegisterUserHandler(userRepository, passwordHasher, clock, rl, httpServletRequest);
    }

    @Test
    void testExecuteRegisterUser_Success() {
        // Given
        RegisterUser req = new RegisterUser("Full Name", "TestUser", "test@example.com", "rawpass", Role.SUPER_ADMIN);
        Long adminId = 123L;

        when(clock.now()).thenReturn(Instant.parse("2025-08-25T10:00:00Z"));
        when(passwordHasher.hash("rawpass")).thenReturn("hashedpass");
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return new User(1L, u.getFullName(), u.getUsername(), u.getEmail(), u.getPasswordHash(), u.getCreatedAt(), u.getRole());
        });

        // When
        Long result = handler.executeRegisterUser(req);

        // Then
        assertEquals(1L, result);
        verify(rl).check(eq("register"), eq(httpServletRequest), eq(null), eq("TestUser"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("Full Name", savedUser.getFullName());
        assertEquals("testuser", savedUser.getUsername()); // lowercased
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("hashedpass", savedUser.getPasswordHash());
        assertEquals(Role.SUPER_ADMIN, savedUser.getRole());
    }

    @Test
    void testExecuteRegisterUser_UsernameAlreadyExists() {
        RegisterUser req = new RegisterUser("Full Name", "TestUser", "test@example.com", "rawpass", Role.SUPER_ADMIN);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> handler.executeRegisterUser(req));

        assertEquals("Username already exists", ex.getMessage());
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testExecuteRegisterUser_EmailAlreadyExists() {
        RegisterUser req = new RegisterUser("Full Name", "TestUser", "test@example.com", "rawpass", Role.SUPER_ADMIN);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> handler.executeRegisterUser(req));

        assertEquals("Email already exists", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testExecuteRegisterUser_DefaultRoleViewerWhenNull() {
        RegisterUser req = new RegisterUser("Full Name", "TestUser", "test@example.com", "rawpass", null);
        when(clock.now()).thenReturn(Instant.parse("2025-08-25T10:00:00Z"));
        when(passwordHasher.hash("rawpass")).thenReturn("hashedpass");
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        handler.executeRegisterUser(req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(Role.VIEWER, captor.getValue().getRole());
    }

    @Test
    void testExecuteRegisterUser_RateLimitThrowsException() {
        RegisterUser req = new RegisterUser("Full Name", "TestUser", "test@example.com", "rawpass", Role.SUPER_ADMIN);
        doThrow(new TooManyRequestsException("register", 30L))
                .when(rl).check(any(), any(), any(), any());

        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                () -> handler.executeRegisterUser(req));

        assertEquals("register", ex.getRuleName());
        assertEquals(30L, ex.getRetryAfterSeconds());
        verify(userRepository, never()).save(any());
    }
}


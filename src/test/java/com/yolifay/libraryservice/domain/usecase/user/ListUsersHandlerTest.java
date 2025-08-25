package com.yolifay.libraryservice.domain.usecase.user;

import com.yolifay.libraryservice.domain.model.Role;
import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.usecase.user.command.ListUsers;
import com.yolifay.libraryservice.domain.usecase.user.handler.ListUsersHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListUsersHandlerTest {

    private UserRepositoryPort userRepo;
    private ListUsersHandler handler;

    @BeforeEach
    void setUp() {
        userRepo = mock(UserRepositoryPort.class);
        handler = new ListUsersHandler(userRepo);
    }

    @Test
    void executeListUser_success() {
        // Given
        ListUsers cmd = new ListUsers(0, 10);
        User user = User.newUser(
                "John Doe",
                "johndoe",
                "john@example.com",
                "hash",
                Instant.now(),
                Role.SUPER_ADMIN
        );
        when(userRepo.findAll(0, 10)).thenReturn(List.of(user));

        // When
        List<User> result = handler.executeListUser(cmd);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("john@example.com", result.get(0).getEmail());
        verify(userRepo, times(1)).findAll(0, 10);
    }

    @Test
    void executeListUser_emptyResult() {
        // Given
        ListUsers cmd = new ListUsers(1, 5);
        when(userRepo.findAll(1, 5)).thenReturn(List.of());

        // When
        List<User> result = handler.executeListUser(cmd);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepo, times(1)).findAll(1, 5);
    }

    @Test
    void executeListUser_repoThrowsException() {
        // Given
        ListUsers cmd = new ListUsers(2, 10);
        when(userRepo.findAll(2, 10)).thenThrow(new RuntimeException("DB error"));

        // When & Then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> handler.executeListUser(cmd));
        assertEquals("DB error", ex.getMessage());
        verify(userRepo, times(1)).findAll(2, 10);
    }
}
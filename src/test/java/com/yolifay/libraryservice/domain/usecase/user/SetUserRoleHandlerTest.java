package com.yolifay.libraryservice.domain.usecase.user;

import com.yolifay.libraryservice.domain.model.Role;
import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.usecase.user.command.SetUserRole;
import com.yolifay.libraryservice.domain.usecase.user.handler.SetUserRoleHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SetUserRoleHandlerTest {

    private final UserRepositoryPort userRepo = mock(UserRepositoryPort.class);
    private SetUserRoleHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SetUserRoleHandler(userRepo);
    }

    // ===== Helper =====
    private User mockUser(long id, String fullName, String username, String email,
                          String passwordHash, Instant createdAt, Role role) {
        User u = mock(User.class);
        when(u.getId()).thenReturn(id);
        when(u.getFullName()).thenReturn(fullName);
        when(u.getUsername()).thenReturn(username);
        when(u.getEmail()).thenReturn(email);
        when(u.getPasswordHash()).thenReturn(passwordHash);
        when(u.getCreatedAt()).thenReturn(createdAt);
        when(u.getRole()).thenReturn(role);
        return u;
    }

    // ================== POSITIVE ==================

    @Test
    void executeSetUserRole_success_copiesAllFields_changesRole_saves_andReturnsSaved() {
        long id = 77L;
        Instant createdAt = Instant.parse("2025-02-01T10:00:00Z");
        User existing = mockUser(id, "Alice A", "alice", "a@x.test", "HASH", createdAt, Role.VIEWER);

        when(userRepo.findById(id)).thenReturn(Optional.of(existing));
        // Kembalikan argumen yang disimpan (simulasi repo)
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        SetUserRole cmd = new SetUserRole(id, Role.EDITOR);

        User out = handler.executeSetUserRole(cmd);

        // capture yang dikirim ke save()
        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(cap.capture());
        User updated = cap.getValue();

        // Semua field tercopy kecuali role
        assertEquals(id, updated.getId());
        assertEquals("Alice A", updated.getFullName());
        assertEquals("alice", updated.getUsername());
        assertEquals("a@x.test", updated.getEmail());
        assertEquals("HASH", updated.getPasswordHash());
        assertEquals(createdAt, updated.getCreatedAt());
        assertEquals(Role.EDITOR, updated.getRole());

        // Return adalah entity hasil save
        assertSame(updated, out);

        verify(userRepo).findById(id);
        verifyNoMoreInteractions(userRepo);
    }

    // ================== NEGATIVES ==================

    @Test
    void executeSetUserRole_userNotFound_throwsIllegalArgumentException() {
        long id = 9L;
        when(userRepo.findById(id)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> handler.executeSetUserRole(new SetUserRole(id, Role.SUPER_ADMIN)));

        assertEquals("User not found", ex.getMessage());
        verify(userRepo).findById(id);
        verify(userRepo, never()).save(any());
        verifyNoMoreInteractions(userRepo);
    }

    @Test
    void executeSetUserRole_repoSaveThrows_isPropagated() {
        long id = 10L;
        Instant createdAt = Instant.parse("2025-02-01T11:00:00Z");
        User existing = mockUser(id, "Bob", "bob", "b@x.test", "HASH2", createdAt, Role.EDITOR);

        when(userRepo.findById(id)).thenReturn(Optional.of(existing));
        when(userRepo.save(any(User.class))).thenThrow(new RuntimeException("db-fail"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> handler.executeSetUserRole(new SetUserRole(id, Role.VIEWER)));
        assertEquals("db-fail", ex.getMessage());

        verify(userRepo).findById(id);
        verify(userRepo).save(any(User.class));
        verifyNoMoreInteractions(userRepo);
    }
}
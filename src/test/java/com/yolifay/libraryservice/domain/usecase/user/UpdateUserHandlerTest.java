package com.yolifay.libraryservice.domain.usecase.user;

import com.yolifay.libraryservice.domain.model.Role;
import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.service.Clock;
import com.yolifay.libraryservice.domain.service.PasswordHasher;
import com.yolifay.libraryservice.domain.usecase.user.command.UpdateUser;
import com.yolifay.libraryservice.domain.usecase.user.handler.UpdateUserHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UpdateUserHandlerTest {

    private final UserRepositoryPort userRepo = mock(UserRepositoryPort.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final Clock clock = mock(Clock.class); // tidak dipakai di handler, tapi tetap diinject

    private UpdateUserHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UpdateUserHandler(userRepo, hasher, clock);
    }

    private User existingUser() {
        return User.builder()
                .id(10L)
                .fullName("Old Name")
                .username("olduser")
                .email("old@mail.test")
                .passwordHash("OLDHASH")
                .createdAt(Instant.parse("2025-02-01T00:00:00Z"))
                .role(Role.VIEWER)
                .build();
    }

    // ============== POSITIVE: update semua field (fullname/email/password) ==============
    @Test
    void executeUpdateUser_updateAllFields_hashesPassword_andLowercasesEmail_andSaves() {
        User current = existingUser();
        when(userRepo.findById(10L)).thenReturn(Optional.of(current));
        when(hasher.hash("NewPW")).thenReturn("NEWHASH");
        // simulasikan repo menyimpan dan mengembalikan objek yang disimpan
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUser cmd = new UpdateUser(10L, "New Name", "NEW@MAIL.TEST", "NewPW");

        User out = handler.executeUpdateUser(cmd);

        // capture argumen yang disimpan untuk memeriksa field
        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(cap.capture());
        User saved = cap.getValue();

        assertEquals(10L, saved.getId());
        assertEquals("New Name", saved.getFullName());              // fullname berubah
        assertEquals("olduser", saved.getUsername());               // username tetap
        assertEquals("new@mail.test", saved.getEmail());            // email dilowercase
        assertEquals("NEWHASH", saved.getPasswordHash());           // password di-hash
        assertEquals(current.getCreatedAt(), saved.getCreatedAt()); // createdAt tetap
        assertEquals(Role.VIEWER, saved.getRole());                 // role tetap

        assertSame(saved, out);
        verify(hasher).hash("NewPW");
        verify(userRepo).findById(10L);
        verifyNoMoreInteractions(userRepo);
    }

    // ============== POSITIVE: semua null / password blank → pakai nilai lama, tanpa hash ==============
    @Test
    void executeUpdateUser_nullFullAndEmail_blankPassword_keepsExistingValues_noHashing() {
        User current = existingUser();
        when(userRepo.findById(10L)).thenReturn(Optional.of(current));
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // fullName=null, email=null, newPassword blank ("   ")
        UpdateUser cmd = new UpdateUser(10L, null, null, "   ");

        User out = handler.executeUpdateUser(cmd);

        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(cap.capture());
        User saved = cap.getValue();

        assertEquals("Old Name", saved.getFullName());    // tetap
        assertEquals("old@mail.test", saved.getEmail());  // tetap
        assertEquals("OLDHASH", saved.getPasswordHash()); // tetap
        assertSame(saved, out);

        verify(hasher, never()).hash(anyString());        // tidak hashing
        verify(userRepo).findById(10L);
        verifyNoMoreInteractions(userRepo);
    }

    // ============== NEGATIVE: user tidak ditemukan ==============
    @Test
    void executeUpdateUser_userNotFound_throwsIllegalArgumentException() {
        when(userRepo.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> handler.executeUpdateUser(new UpdateUser(99L, "X", "x@mail", "pw")));
        assertEquals("User not found", ex.getMessage());

        verify(userRepo).findById(99L);
        verify(userRepo, never()).save(any());
        verify(hasher, never()).hash(anyString());
        verifyNoMoreInteractions(userRepo);
    }
}


package com.yolifay.libraryservice.domain.usecase.user;

import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.usecase.user.command.GetUser;
import com.yolifay.libraryservice.domain.usecase.user.handler.GetUserHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetUserHandlerTest {

    private final UserRepositoryPort userRepo = mock(UserRepositoryPort.class);
    private GetUserHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetUserHandler(userRepo);
    }

    // ===== POSITIVE =====
    @Test
    void executeGetUser_success_returnsUser() {
        long id = 42L;
        User u = mock(User.class);
        when(userRepo.findById(id)).thenReturn(Optional.of(u));

        User out = handler.executeGetUser(new GetUser(id));

        assertSame(u, out);
        verify(userRepo).findById(id);
        verifyNoMoreInteractions(userRepo);
    }

    // ===== NEGATIVE =====
    @Test
    void executeGetUser_notFound_throwsIllegalArgumentException() {
        long id = 99L;
        when(userRepo.findById(id)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> handler.executeGetUser(new GetUser(id))
        );
        assertEquals("User not found", ex.getMessage());
        verify(userRepo).findById(id);
        verifyNoMoreInteractions(userRepo);
    }
}

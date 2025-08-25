package com.yolifay.libraryservice.infrastructure.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrentUserTest {

    @AfterEach
    void tearDown() {
        // Bersihkan context setelah setiap test agar tidak saling mempengaruhi
        SecurityContextHolder.clearContext();
    }

    @Test
    void id_shouldReturnNull_whenAuthenticationIsNull() {
        // Arrange
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(context);

        // Act
        Long result = CurrentUser.id();

        // Assert
        assertNull(result);
        verify(context).getAuthentication();
    }

    @Test
    void id_shouldReturnUserId_whenAuthenticationDetailsIsLong() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(123L);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        // Act
        Long result = CurrentUser.id();

        // Assert
        assertEquals(123L, result);
        verify(context).getAuthentication();
        verify(auth).getDetails();
    }

    @Test
    void id_shouldReturnNull_whenAuthenticationDetailsIsNotLong() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn("not-a-long");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        // Act
        Long result = CurrentUser.id();

        // Assert
        assertNull(result);
        verify(context).getAuthentication();
        verify(auth).getDetails();
    }
}

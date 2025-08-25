package com.yolifay.libraryservice.domain.usecase.library;

import com.yolifay.libraryservice.domain.model.LibraryItem;
import com.yolifay.libraryservice.domain.port.LibraryRepositoryPort;
import com.yolifay.libraryservice.domain.service.Clock;
import com.yolifay.libraryservice.domain.usecase.library.command.CreateLibrary;
import com.yolifay.libraryservice.domain.usecase.library.handler.CreateLibraryHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreateLibraryHandlerTest {

    @Mock
    private LibraryRepositoryPort repo;

    @Mock
    private Clock clock;

    @InjectMocks
    private CreateLibraryHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_success_shouldSaveAndReturnId() {
        // Arrange
        CreateLibrary request = new CreateLibrary("Title", "Content", 99L);
        when(clock.now()).thenReturn(Instant.parse("2025-08-25T10:00:00Z"));

        LibraryItem savedItem = mock(LibraryItem.class);
        when(savedItem.getId()).thenReturn(123L);
        when(repo.save(any(LibraryItem.class))).thenReturn(savedItem);

        // Act
        Long result = handler.execute(request);

        // Assert
        assertEquals(123L, result);

        // Verify clock.now() dipanggil
        verify(clock).now();

        // Verify repo.save() dipanggil dengan LibraryItem yang benar
        ArgumentCaptor<LibraryItem> captor = ArgumentCaptor.forClass(LibraryItem.class);
        verify(repo).save(captor.capture());
        assertEquals("Title", captor.getValue().getTitle());
        assertEquals("Content", captor.getValue().getContent());
        assertEquals(99L, captor.getValue().getAuthorId());
    }

    @Test
    void execute_fail_whenRepoThrowsException() {
        // Arrange
        CreateLibrary request = new CreateLibrary("Title", "Content", 99L);
        when(clock.now()).thenReturn(Instant.parse("2025-08-25T10:00:00Z"));
        when(repo.save(any(LibraryItem.class))).thenThrow(new RuntimeException("DB Error"));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> handler.execute(request));
        assertEquals("DB Error", ex.getMessage());

        verify(clock).now();
        verify(repo).save(any(LibraryItem.class));
    }
}

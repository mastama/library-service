package com.yolifay.libraryservice.domain.usecase.library;

import com.yolifay.libraryservice.domain.model.LibraryItem;
import com.yolifay.libraryservice.domain.port.LibraryRepositoryPort;
import com.yolifay.libraryservice.domain.service.Clock;
import com.yolifay.libraryservice.domain.usecase.library.command.UpdateLibrary;
import com.yolifay.libraryservice.domain.usecase.library.handler.UpdateLibraryHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class UpdateLibraryHandlerTest {
    @Mock
    private LibraryRepositoryPort repo;

    @Mock
    private Clock clock;

    @InjectMocks
    private UpdateLibraryHandler updateHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // --- UpdateLibraryHandler ---
    @Test
    void update_execute_success() {
        UpdateLibrary command = new UpdateLibrary(7L, "NewTitle", "NewContent");
        LibraryItem found = mock(LibraryItem.class);
        LibraryItem updated = mock(LibraryItem.class);

        when(repo.findById(7L)).thenReturn(Optional.of(found));
        when(clock.now()).thenReturn(Instant.parse("2025-08-25T10:00:00Z"));
        when(found.update("NewTitle", "NewContent", Instant.parse("2025-08-25T10:00:00Z"))).thenReturn(updated);

        updateHandler.execute(command);

        verify(repo).findById(7L);
        verify(found).update("NewTitle", "NewContent", Instant.parse("2025-08-25T10:00:00Z"));
        verify(repo).save(updated);
    }

    @Test
    void update_execute_notFound_shouldThrowException() {
        UpdateLibrary command = new UpdateLibrary(77L, "Title", "Content");
        when(repo.findById(77L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> updateHandler.execute(command));

        assertEquals("Library item not found", ex.getMessage());
        verify(repo).findById(77L);
        verify(repo, never()).save(any());
    }

}

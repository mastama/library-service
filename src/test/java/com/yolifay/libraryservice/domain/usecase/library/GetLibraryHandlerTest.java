package com.yolifay.libraryservice.domain.usecase.library;

import com.yolifay.libraryservice.domain.model.LibraryItem;
import com.yolifay.libraryservice.domain.port.LibraryRepositoryPort;
import com.yolifay.libraryservice.domain.service.Clock;
import com.yolifay.libraryservice.domain.usecase.library.command.GetLibrary;
import com.yolifay.libraryservice.domain.usecase.library.handler.GetLibraryHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetLibraryHandlerTest {
    @Mock
    private LibraryRepositoryPort repo;

    @Mock
    private Clock clock;

    @InjectMocks
    private GetLibraryHandler getHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // --- GetLibraryHandler ---
    @Test
    void get_execute_success() {
        GetLibrary query = new GetLibrary(5L);
        LibraryItem item = mock(LibraryItem.class);
        when(repo.findById(5L)).thenReturn(Optional.of(item));

        LibraryItem result = getHandler.execute(query);

        assertSame(item, result);
        verify(repo).findById(5L);
    }

    @Test
    void get_execute_notFound_shouldThrowException() {
        GetLibrary query = new GetLibrary(99L);
        when(repo.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> getHandler.execute(query));

        assertEquals("Library item not found", ex.getMessage());
        verify(repo).findById(99L);
    }

}

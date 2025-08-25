package com.yolifay.libraryservice.domain.usecase.library;

import com.yolifay.libraryservice.domain.model.LibraryItem;
import com.yolifay.libraryservice.domain.port.LibraryRepositoryPort;
import com.yolifay.libraryservice.domain.service.Clock;
import com.yolifay.libraryservice.domain.usecase.library.command.ListLibrary;
import com.yolifay.libraryservice.domain.usecase.library.handler.ListLibraryHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ListLibraryHandlerTest {
    @Mock
    private LibraryRepositoryPort repo;

    @Mock
    private Clock clock;

    @InjectMocks
    private ListLibraryHandler listHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // --- ListLibraryHandler ---
    @Test
    void list_execute_success() {
        ListLibrary query = new ListLibrary(1, 10);
        List<LibraryItem> expected = List.of(mock(LibraryItem.class));
        when(repo.findAll(1, 10)).thenReturn(expected);

        List<LibraryItem> result = listHandler.execute(query);

        assertEquals(expected, result);
        verify(repo).findAll(1, 10);
    }

}

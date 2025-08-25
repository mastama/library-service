package com.yolifay.libraryservice.domain.usecase.library;

import com.yolifay.libraryservice.domain.port.LibraryRepositoryPort;
import com.yolifay.libraryservice.domain.service.Clock;
import com.yolifay.libraryservice.domain.usecase.library.command.DeleteLibrary;
import com.yolifay.libraryservice.domain.usecase.library.handler.DeleteLibraryHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

class DeleteLibraryHandlerTest {
    @Mock
    private LibraryRepositoryPort repo;

    @Mock
    private Clock clock;

    @InjectMocks
    private DeleteLibraryHandler deleteHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // --- DeleteLibraryHandler ---
    @Test
    void delete_execute_success() {
        DeleteLibrary request = new DeleteLibrary(10L);

        deleteHandler.execute(request);

        verify(repo).deleteById(10L);
    }

}

package com.yolifay.libraryservice.domain.usecase.library.handler;

import com.yolifay.libraryservice.domain.port.LibraryRepositoryPort;
import com.yolifay.libraryservice.domain.usecase.library.command.DeleteLibrary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeleteLibraryHandler {
    private final LibraryRepositoryPort repo;

    public void execute(DeleteLibrary c){ repo.deleteById(c.id()); }
}

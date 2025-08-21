package com.yolifay.libraryservice.domain.usecase.library.handler;

import com.yolifay.libraryservice.domain.model.LibraryItem;
import com.yolifay.libraryservice.domain.port.LibraryRepositoryPort;
import com.yolifay.libraryservice.domain.usecase.library.command.GetLibrary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetLibraryHandler {
    private final LibraryRepositoryPort repo;

    public LibraryItem execute(GetLibrary q){
        return repo.findById(q.id())
                .orElseThrow(() -> new IllegalArgumentException("Library item not found"));
    }
}

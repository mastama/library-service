package com.yolifay.libraryservice.domain.usecase.library.handler;

import com.yolifay.libraryservice.domain.model.LibraryItem;
import com.yolifay.libraryservice.domain.port.LibraryRepositoryPort;
import com.yolifay.libraryservice.domain.usecase.library.command.ListLibrary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ListLibraryHandler {
    private final LibraryRepositoryPort repo;

    public List<LibraryItem> execute(ListLibrary q){
        return repo.findAll(q.page(), q.size());
    }
}

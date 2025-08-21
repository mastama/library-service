package com.yolifay.libraryservice.domain.usecase.library.handler;

import com.yolifay.libraryservice.domain.model.LibraryItem;
import com.yolifay.libraryservice.domain.port.LibraryRepositoryPort;
import com.yolifay.libraryservice.domain.service.Clock;
import com.yolifay.libraryservice.domain.usecase.library.command.UpdateLibrary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateLibraryHandler {
    private final LibraryRepositoryPort repo;
    private final Clock clock;

    public void execute(UpdateLibrary c){
        LibraryItem found = repo.findById(c.id())
                .orElseThrow(() -> new IllegalArgumentException("Library item not found"));
        repo.save(found.update(c.title(), c.content(), clock.now()));
    }
}

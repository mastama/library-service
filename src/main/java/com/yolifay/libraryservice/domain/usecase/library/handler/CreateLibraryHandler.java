package com.yolifay.libraryservice.domain.usecase.library.handler;

import com.yolifay.libraryservice.domain.model.LibraryItem;
import com.yolifay.libraryservice.domain.port.LibraryRepositoryPort;
import com.yolifay.libraryservice.domain.service.Clock;
import com.yolifay.libraryservice.domain.usecase.library.command.CreateLibrary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateLibraryHandler {
    private final LibraryRepositoryPort repo;
    private final Clock clock;

    public Long execute(CreateLibrary c){
        LibraryItem item = LibraryItem.create(c.title(), c.content(), c.authorId(), clock.now());
        return repo.save(item).getId();
    }
}

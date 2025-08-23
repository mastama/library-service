package com.yolifay.libraryservice.infrastructure.web;

import com.yolifay.libraryservice.application.dto.CreateLibraryRequest;
import com.yolifay.libraryservice.application.dto.IdResponse;
import com.yolifay.libraryservice.application.dto.LibraryResponse;
import com.yolifay.libraryservice.application.dto.UpdateLibraryRequest;
import com.yolifay.libraryservice.domain.model.LibraryItem;
import com.yolifay.libraryservice.domain.usecase.library.command.*;
import com.yolifay.libraryservice.domain.usecase.library.handler.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/library")
@RequiredArgsConstructor
@Slf4j
public class LibraryController {

    private final CreateLibraryHandler createHandler;
    private final UpdateLibraryHandler updateHandler;
    private final DeleteLibraryHandler deleteHandler;
    private final GetLibraryHandler getHandler;
    private final ListLibraryHandler listHandler;

    @PostMapping
    public IdResponse createLibrary(@Valid @RequestBody CreateLibraryRequest request) {
        log.info("Incoming Create library: {}", request.title());
        Long id = createHandler.execute(new CreateLibrary(
                request.title(),
                request.content(),
                request.authorId()));
        log.info("Outgoing Created library: {}", request.title());
        return new IdResponse(id);
    }

    @PutMapping("/{id}")
    public void updateLibrary(@PathVariable Long id, @Valid @RequestBody UpdateLibraryRequest request) {
        log.info("Incoming Update library: {}", id);
        log.info("Outgoing Update library: {}", id);
        updateHandler.execute(new UpdateLibrary(id, request.title(), request.content()));
    }

    @DeleteMapping("/{id}")
    public void deleteLibrary(@PathVariable Long id) {
        log.info("Incoming Delete library: {}", id);
        log.info("Outgoing Delete library: {}", id);
        deleteHandler.execute(new DeleteLibrary(id));
    }

    @GetMapping("/{id}")
    public LibraryResponse getLibrary(@PathVariable Long id) {
        log.info("Incoming get library by id: {}", id);
        LibraryItem item = getHandler.execute(new GetLibrary(id));
        log.info("Outgoing get library by id: {}", id);
        return new LibraryResponse(item.getId(), item.getTitle(), item.getContent(), item.getAuthorId(),
                item.getCreatedAt(), item.getUpdatedAt());
    }

    @GetMapping
    public List<LibraryResponse> listLibraries(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        log.info("Incoming list library page: {}, size: {}", page, size);
        log.info("Outgoing list library page: {}, size: {}", page, size);
        return listHandler.execute(new ListLibrary(page, size)).stream()
                .map(item -> new LibraryResponse(item.getId(), item.getTitle(), item.getContent(), item.getAuthorId(),
                        item.getCreatedAt(), item.getUpdatedAt()))
                .toList();
    }
}

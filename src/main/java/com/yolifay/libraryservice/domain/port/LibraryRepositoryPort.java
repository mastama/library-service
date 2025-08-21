package com.yolifay.libraryservice.domain.port;

import com.yolifay.libraryservice.domain.model.LibraryItem;

import java.util.List;
import java.util.Optional;

public interface LibraryRepositoryPort {
    LibraryItem save(LibraryItem libraryItem);
    Optional<LibraryItem> findById(Long id);
    List<LibraryItem> findAll(int page, int size);
    void deleteById(Long id);
}

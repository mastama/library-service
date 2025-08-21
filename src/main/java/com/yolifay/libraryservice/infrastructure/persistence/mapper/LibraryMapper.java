package com.yolifay.libraryservice.infrastructure.persistence.mapper;

import com.yolifay.libraryservice.domain.model.LibraryItem;
import com.yolifay.libraryservice.infrastructure.persistence.entity.LibraryItemEntity;

public class LibraryMapper {

    private LibraryMapper() {
        // Private constructor to prevent instantiation
    }

    public static LibraryItemEntity toEntity(LibraryItem d) {
        LibraryItemEntity e = new LibraryItemEntity();
        e.setId(d.getId());
        e.setTitle(d.getTitle());
        e.setContent(d.getContent());
        e.setAuthorId(d.getAuthorId());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        return e;
    }

    public static LibraryItem toDomain(LibraryItemEntity e) {
        return new LibraryItem(
                e.getId(),
                e.getTitle(),
                e.getContent(),
                e.getAuthorId(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}

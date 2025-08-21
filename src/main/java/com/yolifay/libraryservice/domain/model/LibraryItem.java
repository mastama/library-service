package com.yolifay.libraryservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class LibraryItem {
    private final Long id;
    private final String title;
    private final String content;
    private final Long authorId;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static LibraryItem create(String title, String content, Long authorId, Instant now) {
        return new LibraryItem(null, title, content, authorId, now, now);
    }

    public LibraryItem update(String title, String content, Instant now) {
        return new LibraryItem(this.id, title, content, this.authorId, this.createdAt, now);
    }
}

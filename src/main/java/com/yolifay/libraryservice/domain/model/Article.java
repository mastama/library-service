package com.yolifay.libraryservice.domain.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter @AllArgsConstructor @Builder
public class Article {
    private final Long id;
    private final String title;
    private final String content;
    private final Long authorId;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static Article create(String title, String content, Long authorId, Instant now) {
        return new Article(null, title, content, authorId, now, now);
    }

    public Article update(String title, String content, Instant now) {
        return new Article(this.id, title, content, this.authorId, this.createdAt, now);
    }
}

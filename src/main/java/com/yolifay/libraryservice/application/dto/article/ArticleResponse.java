package com.yolifay.libraryservice.application.dto.article;

import java.time.Instant;

public record ArticleResponse(
        Long id,
        String title,
        String content,
        Long authorId,
        Instant createdAt,
        Instant updatedAt
) {
}

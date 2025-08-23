package com.yolifay.libraryservice.application.dto;

import java.time.Instant;

public record LibraryResponse(
        Long id,
        String title,
        String content,
        Long authorId,
        Instant createdAt,
        Instant updatedAt
) {
}

package com.yolifay.libraryservice.application.dto.article;

import jakarta.validation.constraints.NotBlank;

public record CreateArticleRequest(
        @NotBlank String title,
        @NotBlank String content
) {
}

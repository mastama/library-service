package com.yolifay.libraryservice.application.dto.article;

import jakarta.validation.constraints.NotBlank;

public record UpdateArticleRequest(
        @NotBlank String title,
        @NotBlank String content
) {
}

package com.yolifay.libraryservice.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateLibraryRequest(
    @NotBlank(message = "Library title must not be blank") String title,
    @NotBlank(message = "Library content must not be blank") String content,
    @NotNull(message = "Author ID must not be null") Long authorId
) {}

package com.yolifay.libraryservice.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateLibraryRequest(
        @NotBlank (message = "Library title must not be blank") String title,
        @NotBlank (message = "Library content must not be blank") String content
) {
}

package com.yolifay.libraryservice.domain.usecase.library.command;

public record CreateLibrary(String title, String content, Long authorId) {
}

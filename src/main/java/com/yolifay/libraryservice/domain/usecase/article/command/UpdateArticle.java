package com.yolifay.libraryservice.domain.usecase.article.command;

public record UpdateArticle(Long id, String title, String content, Long authorId) {
}

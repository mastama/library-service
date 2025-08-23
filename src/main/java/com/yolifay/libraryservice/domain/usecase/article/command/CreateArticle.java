package com.yolifay.libraryservice.domain.usecase.article.command;

public record CreateArticle(String title, String content, Long authorId) {
}

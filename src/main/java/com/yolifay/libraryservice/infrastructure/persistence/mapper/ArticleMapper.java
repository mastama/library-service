package com.yolifay.libraryservice.infrastructure.persistence.mapper;

import com.yolifay.libraryservice.domain.model.Article;
import com.yolifay.libraryservice.infrastructure.persistence.entity.ArticleEntity;

public final class ArticleMapper {
    private ArticleMapper() {}

    public static ArticleEntity toArticleEntity(Article a) {
        var articleEntity = new ArticleEntity();
        articleEntity.setId(a.getId());
        articleEntity.setTitle(a.getTitle());
        articleEntity.setAuthorId(a.getAuthorId());
        articleEntity.setContent(a.getContent());
        articleEntity.setCreatedAt(a.getCreatedAt());
        articleEntity.setUpdatedAt(a.getUpdatedAt());

        return articleEntity;
    }

    public static Article toDomain(ArticleEntity e) {
        return new Article(
                e.getId(),
                e.getTitle(),
                e.getContent(),
                e.getAuthorId(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}

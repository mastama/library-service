package com.yolifay.libraryservice.domain.usecase.article.handler;

import com.yolifay.libraryservice.domain.model.Article;
import com.yolifay.libraryservice.domain.port.ArticleRepositoryPort;
import com.yolifay.libraryservice.domain.usecase.article.command.GetArticle;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetArticleHandler {
    private final ArticleRepositoryPort articleRepo;

    @Cacheable(cacheNames = "articles.byId", key = "#q.id")
    public Article executeGetArticle(GetArticle q){
        return articleRepo.findById(q.id()).orElseThrow(() -> new IllegalArgumentException("Article not found"));
    }
}

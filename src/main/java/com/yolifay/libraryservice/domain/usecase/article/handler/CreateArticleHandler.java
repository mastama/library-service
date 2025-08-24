package com.yolifay.libraryservice.domain.usecase.article.handler;

import com.yolifay.libraryservice.domain.model.Article;
import com.yolifay.libraryservice.domain.port.ArticleRepositoryPort;
import com.yolifay.libraryservice.domain.service.Clock;
import com.yolifay.libraryservice.domain.usecase.article.command.CreateArticle;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class CreateArticleHandler {
    private final ArticleRepositoryPort articleRepo;
    private final Clock clock;

    // Saat write, evict cache
    @CacheEvict(cacheNames = {"articles.byId","articles.list"}, allEntries = true)
    public Article executeCreateArticle(CreateArticle ca){
        Article a = Article.create(ca.title(), ca.content(), ca.authorId(), clock.now());
        return articleRepo.save(a);
    }
}

package com.yolifay.libraryservice.domain.usecase.article.handler;

import com.yolifay.libraryservice.domain.model.Article;
import com.yolifay.libraryservice.domain.port.ArticleRepositoryPort;
import com.yolifay.libraryservice.domain.service.Clock;
import com.yolifay.libraryservice.domain.usecase.article.command.UpdateArticle;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateArticleHandler {
    private final ArticleRepositoryPort articleRepo;
    private final Clock clock;

    @CacheEvict(cacheNames = {"articles.byId","articles.list"}, allEntries = true)
    public void executeUpdateArticle(UpdateArticle ua){
        Article found = articleRepo.findById(ua.id()).orElseThrow(() -> new IllegalArgumentException("Article not found"));
        if (!found.getAuthorId().equals(ua.authorId())) throw new IllegalArgumentException("Forbidden");
        articleRepo.save(found.update(ua.title(), ua.content(), clock.now()));
    }
}

package com.yolifay.libraryservice.domain.usecase.article.handler;

import com.yolifay.libraryservice.domain.model.Article;
import com.yolifay.libraryservice.domain.port.ArticleRepositoryPort;
import com.yolifay.libraryservice.domain.service.Clock;
import com.yolifay.libraryservice.domain.usecase.article.command.UpdateArticle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateArticleHandler {
    private final ArticleRepositoryPort articleRepo;
    private final Clock clock;

    public void executeUpdateArticle(UpdateArticle updateArticle){
        Article found = articleRepo.findById(updateArticle.id()).orElseThrow(() -> new IllegalArgumentException("Article not found"));
        if (!found.getAuthorId().equals(updateArticle.authorId())) throw new IllegalArgumentException("Forbidden");
        articleRepo.save(found.update(updateArticle.title(), updateArticle.content(), clock.now()));
    }
}

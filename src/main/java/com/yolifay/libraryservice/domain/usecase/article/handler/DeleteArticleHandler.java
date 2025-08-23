package com.yolifay.libraryservice.domain.usecase.article.handler;

import com.yolifay.libraryservice.domain.model.Article;
import com.yolifay.libraryservice.domain.port.ArticleRepositoryPort;
import com.yolifay.libraryservice.domain.usecase.article.command.DeleteArticle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeleteArticleHandler {
    private final ArticleRepositoryPort articleRepo;

    public void executeDeleteArticle(DeleteArticle q) {
        Article found = articleRepo.findById(q.id()).orElseThrow(() -> new IllegalArgumentException("Article not found"));
        if (!found.getAuthorId().equals(q.authorId())) throw new IllegalArgumentException("Forbidden");
        articleRepo.deleteById(q.id());
    }
}

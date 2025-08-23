package com.yolifay.libraryservice.domain.usecase.article.handler;

import com.yolifay.libraryservice.domain.model.Article;
import com.yolifay.libraryservice.domain.port.ArticleRepositoryPort;
import com.yolifay.libraryservice.domain.usecase.article.command.ListArticles;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ListArticlesHandler {
    private final ArticleRepositoryPort articleRepo;

    public List<Article> executeListArticles(ListArticles q){
        return articleRepo.findAll(q.page(), q.size());
    }
}

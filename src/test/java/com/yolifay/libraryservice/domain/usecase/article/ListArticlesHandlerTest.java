package com.yolifay.libraryservice.domain.usecase.article;

import com.yolifay.libraryservice.domain.model.Article;
import com.yolifay.libraryservice.domain.port.ArticleRepositoryPort;
import com.yolifay.libraryservice.domain.usecase.article.command.ListArticles;
import com.yolifay.libraryservice.domain.usecase.article.handler.ListArticlesHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ListArticlesHandlerTest {

    private ArticleRepositoryPort articleRepo;
    private ListArticlesHandler handler;

    @BeforeEach
    void setUp() {
        articleRepo = mock(ArticleRepositoryPort.class);
        handler = new ListArticlesHandler(articleRepo);
    }

    @Test
    void executeListArticles_shouldReturnArticles() {
        // given
        int page = 0;
        int size = 2;
        ListArticles q = new ListArticles(page, size);

        Article article = Article.builder()
                .id(1L)
                .authorId(10L)
                .title("Title")
                .content("Content")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(articleRepo.findAll(page, size)).thenReturn(List.of(article));

        // when
        List<Article> result = handler.executeListArticles(q);

        // then
        assertEquals(1, result.size());
        assertEquals(article, result.get(0));
        verify(articleRepo).findAll(page, size);
    }

    @Test
    void executeListArticles_shouldReturnEmptyList() {
        // given
        int page = 1;
        int size = 5;
        ListArticles q = new ListArticles(page, size);

        when(articleRepo.findAll(page, size)).thenReturn(Collections.emptyList());

        // when
        List<Article> result = handler.executeListArticles(q);

        // then
        assertEquals(0, result.size());
        verify(articleRepo).findAll(page, size);
    }
}

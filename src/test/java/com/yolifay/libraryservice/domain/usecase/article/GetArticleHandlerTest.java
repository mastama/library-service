package com.yolifay.libraryservice.domain.usecase.article;

import com.yolifay.libraryservice.domain.model.Article;
import com.yolifay.libraryservice.domain.port.ArticleRepositoryPort;
import com.yolifay.libraryservice.domain.usecase.article.command.GetArticle;
import com.yolifay.libraryservice.domain.usecase.article.handler.GetArticleHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetArticleHandlerTest {

    private final ArticleRepositoryPort repo = mock(ArticleRepositoryPort.class);
    private GetArticleHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetArticleHandler(repo);
    }

    // ====== POSITIVE ======
    @Test
    void executeGetArticle_success_returnsArticle() {
        long id = 123L;
        Instant now = Instant.parse("2025-02-01T10:00:00Z");
        Article art = new Article(id, "Title", "Content", 9L, now, now);

        when(repo.findById(id)).thenReturn(Optional.of(art));

        Article out = handler.executeGetArticle(new GetArticle(id));

        assertSame(art, out);
        verify(repo).findById(id);
        verifyNoMoreInteractions(repo);
    }

    // ====== NEGATIVE ======
    @Test
    void executeGetArticle_notFound_throwsIllegalArgumentException() {
        long id = 9L;
        when(repo.findById(id)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> handler.executeGetArticle(new GetArticle(id))
        );
        assertEquals("Article not found", ex.getMessage());
        verify(repo).findById(id);
        verifyNoMoreInteractions(repo);
    }
}


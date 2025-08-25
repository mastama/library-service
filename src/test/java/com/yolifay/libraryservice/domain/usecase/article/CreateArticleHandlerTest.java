package com.yolifay.libraryservice.domain.usecase.article;

import com.yolifay.libraryservice.domain.model.Article;
import com.yolifay.libraryservice.domain.port.ArticleRepositoryPort;
import com.yolifay.libraryservice.domain.service.Clock;
import com.yolifay.libraryservice.domain.usecase.article.command.CreateArticle;
import com.yolifay.libraryservice.domain.usecase.article.handler.CreateArticleHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreateArticleHandlerTest {

    private final ArticleRepositoryPort repo = mock(ArticleRepositoryPort.class);
    private final Clock clock = mock(Clock.class);

    private CreateArticleHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreateArticleHandler(repo, clock);
    }

    @Test
    void executeCreateArticle_success_buildsArticleWithClockNow_andSaves() {
        // arrange
        Instant now = Instant.parse("2025-02-01T10:00:00Z");
        when(clock.now()).thenReturn(now);

        CreateArticle cmd = new CreateArticle("Title", "Content", 9L);

        // repo mengembalikan entity yg sudah punya id
        Article saved = new Article(123L, "Title", "Content", 9L, now, now);
        when(repo.save(any(Article.class))).thenReturn(saved);

        // act
        Article out = handler.executeCreateArticle(cmd);

        // assert: nilai yang dilempar ke repo
        ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
        verify(repo).save(captor.capture());
        Article passed = captor.getValue();

        assertNull(passed.getId(), "id harus null saat create");
        assertEquals("Title", passed.getTitle());
        assertEquals("Content", passed.getContent());
        assertEquals(9L, passed.getAuthorId());
        assertEquals(now, passed.getCreatedAt());
        assertEquals(now, passed.getUpdatedAt());

        // dan nilai return adalah yang dikembalikan repo
        assertSame(saved, out);
        verify(clock).now();
    }

    @Test
    void executeCreateArticle_repoThrows_propagatesException() {
        Instant now = Instant.parse("2025-02-01T10:00:00Z");
        when(clock.now()).thenReturn(now);

        when(repo.save(any(Article.class)))
                .thenThrow(new RuntimeException("DB down"));

        CreateArticle cmd = new CreateArticle("T", "C", 1L);

        RuntimeException ex =
                assertThrows(RuntimeException.class, () -> handler.executeCreateArticle(cmd));
        assertEquals("DB down", ex.getMessage());

        verify(clock).now();
        verify(repo).save(any(Article.class));
    }
}

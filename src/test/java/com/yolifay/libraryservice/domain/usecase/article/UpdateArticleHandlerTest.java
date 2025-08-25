package com.yolifay.libraryservice.domain.usecase.article;

import com.yolifay.libraryservice.domain.model.Article;
import com.yolifay.libraryservice.domain.port.ArticleRepositoryPort;
import com.yolifay.libraryservice.domain.service.Clock;
import com.yolifay.libraryservice.domain.usecase.article.command.UpdateArticle;
import com.yolifay.libraryservice.domain.usecase.article.handler.UpdateArticleHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UpdateArticleHandlerTest {

    private ArticleRepositoryPort articleRepo;
    private Clock clock;
    private UpdateArticleHandler handler;

    @BeforeEach
    void setUp() {
        articleRepo = mock(ArticleRepositoryPort.class);
        clock = mock(Clock.class);
        handler = new UpdateArticleHandler(articleRepo, clock);
    }

    @Test
    void executeUpdateArticle_shouldUpdateSuccessfully() {
        // given
        Long articleId = 1L;
        Long authorId = 10L;
        Instant now = Instant.now();

        Article existing = Article.builder()
                .id(articleId)
                .title("Old Title")
                .content("Old Content")
                .authorId(authorId)
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now().minusSeconds(3600))
                .build();

        UpdateArticle ua = new UpdateArticle(articleId, "New Title", "New Content", authorId);

        when(articleRepo.findById(articleId)).thenReturn(Optional.of(existing));
        when(clock.now()).thenReturn(now);

        // when
        handler.executeUpdateArticle(ua);

        // then
        verify(articleRepo).findById(articleId);
        verify(clock).now();
        verify(articleRepo).save(argThat(updated ->
                updated.getId().equals(articleId) &&
                        updated.getTitle().equals("New Title") &&
                        updated.getContent().equals("New Content") &&
                        updated.getAuthorId().equals(authorId) &&
                        updated.getUpdatedAt().equals(now)
        ));
    }

    @Test
    void executeUpdateArticle_shouldThrowWhenArticleNotFound() {
        // given
        Long articleId = 1L;
        UpdateArticle ua = new UpdateArticle(articleId, "Title", "Content", 10L);

        when(articleRepo.findById(articleId)).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> handler.executeUpdateArticle(ua));
        assertEquals("Article not found", ex.getMessage());

        verify(articleRepo).findById(articleId);
        verifyNoInteractions(clock);
        verify(articleRepo, never()).save(any());
    }

    @Test
    void executeUpdateArticle_shouldThrowWhenAuthorForbidden() {
        // given
        Long articleId = 1L;
        Long realAuthorId = 10L;
        Long wrongAuthorId = 99L;

        Article existing = Article.builder()
                .id(articleId)
                .title("Old Title")
                .content("Old Content")
                .authorId(realAuthorId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        UpdateArticle ua = new UpdateArticle(articleId, "New Title", "New Content", wrongAuthorId);

        when(articleRepo.findById(articleId)).thenReturn(Optional.of(existing));

        // when & then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> handler.executeUpdateArticle(ua));
        assertEquals("Forbidden", ex.getMessage());

        verify(articleRepo).findById(articleId);
        verifyNoInteractions(clock);
        verify(articleRepo, never()).save(any());
    }
}

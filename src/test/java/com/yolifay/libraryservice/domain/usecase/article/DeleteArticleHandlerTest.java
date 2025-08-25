package com.yolifay.libraryservice.domain.usecase.article;

import com.yolifay.libraryservice.domain.model.Article;
import com.yolifay.libraryservice.domain.port.ArticleRepositoryPort;
import com.yolifay.libraryservice.domain.usecase.article.command.DeleteArticle;
import com.yolifay.libraryservice.domain.usecase.article.handler.DeleteArticleHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeleteArticleHandlerTest {

    private ArticleRepositoryPort articleRepo;
    private DeleteArticleHandler handler;

    @BeforeEach
    void setUp() {
        articleRepo = mock(ArticleRepositoryPort.class);
        handler = new DeleteArticleHandler(articleRepo);
    }

    @Test
    void executeDeleteArticle_successfulDeletion() {
        // given
        Long articleId = 1L;
        Long authorId = 99L;
        Article article = Article.builder()
                .id(articleId)
                .authorId(authorId)
                .title("Test")
                .content("Content")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        DeleteArticle q = new DeleteArticle(articleId, authorId);
        when(articleRepo.findById(articleId)).thenReturn(Optional.of(article));

        // when
        handler.executeDeleteArticle(q);

        // then
        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        verify(articleRepo).deleteById(idCaptor.capture());
        assertEquals(articleId, idCaptor.getValue());
        verify(articleRepo).findById(articleId);
    }

    @Test
    void executeDeleteArticle_articleNotFound_shouldThrow() {
        // given
        Long articleId = 1L;
        Long authorId = 99L;
        DeleteArticle q = new DeleteArticle(articleId, authorId);
        when(articleRepo.findById(articleId)).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> handler.executeDeleteArticle(q));
        assertEquals("Article not found", ex.getMessage());
        verify(articleRepo, never()).deleteById(any());
    }

    @Test
    void executeDeleteArticle_forbidden_shouldThrow() {
        // given
        Long articleId = 1L;
        Long authorId = 99L;
        Long otherAuthorId = 88L;

        Article article = Article.builder()
                .id(articleId)
                .authorId(otherAuthorId)
                .title("Test")
                .content("Content")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        DeleteArticle q = new DeleteArticle(articleId, authorId);
        when(articleRepo.findById(articleId)).thenReturn(Optional.of(article));

        // when & then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> handler.executeDeleteArticle(q));
        assertEquals("Forbidden", ex.getMessage());
        verify(articleRepo, never()).deleteById(any());
    }
}
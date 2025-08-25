package com.yolifay.libraryservice.infrastructure.web;

import com.yolifay.libraryservice.application.dto.article.ArticleResponse;
import com.yolifay.libraryservice.application.dto.article.CreateArticleRequest;
import com.yolifay.libraryservice.application.dto.article.UpdateArticleRequest;
import com.yolifay.libraryservice.domain.model.Article;
import com.yolifay.libraryservice.domain.usecase.article.command.*;
import com.yolifay.libraryservice.domain.usecase.article.handler.*;
import com.yolifay.libraryservice.infrastructure.security.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleControllerTest {

    private final CreateArticleHandler create = mock(CreateArticleHandler.class);
    private final UpdateArticleHandler update = mock(UpdateArticleHandler.class);
    private final DeleteArticleHandler delete = mock(DeleteArticleHandler.class);
    private final GetArticleHandler get = mock(GetArticleHandler.class);
    private final ListArticlesHandler list = mock(ListArticlesHandler.class);

    private ArticleController controller;

    @BeforeEach
    void setUp() {
        controller = new ArticleController(create, update, delete, get, list);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /* ===== Helpers ===== */

    private void putAuth(String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                "user", "pwd",
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Article mockArticle(long id, long authorId, String title, String content) {
        Article a = mock(Article.class);
        when(a.getId()).thenReturn(id);
        when(a.getAuthorId()).thenReturn(authorId);
        when(a.getTitle()).thenReturn(title);
        when(a.getContent()).thenReturn(content);
        Instant now = Instant.now();
        when(a.getCreatedAt()).thenReturn(now);
        when(a.getUpdatedAt()).thenReturn(now);
        return a;
    }

    /* ====== CREATE ====== */

    @Test
    void create_success_withAuth_covers_logging_and_mapping() {
        putAuth("EDITOR");
        CreateArticleRequest req = mock(CreateArticleRequest.class);
        when(req.title()).thenReturn("T");
        when(req.content()).thenReturn("C");

        Article saved = mockArticle(101L, 99L, "T", "C");

        try (MockedStatic<CurrentUser> cu = Mockito.mockStatic(CurrentUser.class)) {
            cu.when(CurrentUser::id).thenReturn(99L);
            when(create.executeCreateArticle(any(CreateArticle.class))).thenReturn(saved);

            ResponseEntity<ArticleResponse> resp = controller.create(req);

            assertEquals(201, resp.getStatusCodeValue());
            assertEquals(URI.create("/api/v1/articles/101"), resp.getHeaders().getLocation());
            ArticleResponse body = resp.getBody();
            assertNotNull(body);
            assertEquals(101L, body.id());
            assertEquals("T", body.title());
            assertEquals("C", body.content());
            assertEquals(99L, body.authorId());

            verify(create).executeCreateArticle(any(CreateArticle.class));
        }
    }

    /* ====== UPDATE ====== */

    @Test
    void update_success_withAuth_fetches_updated_entity() {
        putAuth("EDITOR");
        UpdateArticleRequest req = mock(UpdateArticleRequest.class);
        when(req.title()).thenReturn("NewT");
        when(req.content()).thenReturn("NewC");

        Article updated = mockArticle(5L, 10L, "NewT", "NewC");

        try (MockedStatic<CurrentUser> cu = Mockito.mockStatic(CurrentUser.class)) {
            cu.when(CurrentUser::id).thenReturn(10L);

            // update handler tidak mengembalikan apa-apa; cukup pastikan dipanggil
            doNothing().when(update).executeUpdateArticle(any(UpdateArticle.class));
            when(get.executeGetArticle(any(GetArticle.class))).thenReturn(updated);

            ResponseEntity<ArticleResponse> resp = controller.update(5L, req);

            assertEquals(200, resp.getStatusCodeValue());
            ArticleResponse body = resp.getBody();
            assertNotNull(body);
            assertEquals(5L, body.id());
            assertEquals("NewT", body.title());
            assertEquals("NewC", body.content());
            assertEquals(10L, body.authorId());

            verify(update).executeUpdateArticle(any(UpdateArticle.class));
            verify(get).executeGetArticle(any(GetArticle.class));
        }
    }

    /* ====== DELETE ====== */

    @Test
    void delete_success_withAuth_returns_noContent() {
        putAuth("SUPER_ADMIN");

        try (MockedStatic<CurrentUser> cu = Mockito.mockStatic(CurrentUser.class)) {
            cu.when(CurrentUser::id).thenReturn(7L);
            doNothing().when(delete).executeDeleteArticle(any(DeleteArticle.class));

            ResponseEntity<Void> resp = controller.delete(77L);

            assertEquals(204, resp.getStatusCodeValue());
            assertFalse(resp.hasBody());
            verify(delete).executeDeleteArticle(any(DeleteArticle.class));
        }
    }

    /* ====== GET ====== */

    @Test
    void get_success_withAuth() {
        putAuth("VIEWER");
        Article a = mockArticle(3L, 2L, "Hello", "World");

        try (MockedStatic<CurrentUser> cu = Mockito.mockStatic(CurrentUser.class)) {
            cu.when(CurrentUser::id).thenReturn(2L);
            when(get.executeGetArticle(any(GetArticle.class))).thenReturn(a);

            ResponseEntity<ArticleResponse> resp = controller.get(3L);

            assertEquals(200, resp.getStatusCodeValue());
            assertEquals(3L, resp.getBody().id());
            verify(get).executeGetArticle(any(GetArticle.class));
        }
    }

    /* ====== LIST ====== */

    @Test
    void list_success_nonEmpty_withAuth() {
        putAuth("EDITOR");
        Article a1 = mockArticle(1L, 9L, "A1", "C1");
        Article a2 = mockArticle(2L, 9L, "A2", "C2");

        try (MockedStatic<CurrentUser> cu = Mockito.mockStatic(CurrentUser.class)) {
            cu.when(CurrentUser::id).thenReturn(9L);
            when(list.executeListArticles(any(ListArticles.class))).thenReturn(List.of(a1, a2));

            ResponseEntity<List<ArticleResponse>> resp = controller.list(0, 2);

            assertEquals(200, resp.getStatusCodeValue());
            assertNotNull(resp.getBody());
            assertEquals(2, resp.getBody().size());
            assertEquals(1L, resp.getBody().get(0).id());
            assertEquals(2L, resp.getBody().get(1).id());

            verify(list).executeListArticles(any(ListArticles.class));
        }
    }

    /* ====== NEGATIVE path untuk currentRole(): auth == null ====== */

    @Test
    void currentRole_whenNoAuthentication_returnsDash_and_endpoints_still_work() {
        // pastikan context kosong -> cabang (auth == null) ter-cover
        SecurityContextHolder.clearContext();

        Article a = mockArticle(10L, 88L, "Title", "Cnt");
        when(get.executeGetArticle(any(GetArticle.class))).thenReturn(a);
        // CurrentUser.id() dipakai tapi nilainya tidak mempengaruhi get/list body
        try (MockedStatic<CurrentUser> cu = Mockito.mockStatic(CurrentUser.class)) {
            cu.when(CurrentUser::id).thenReturn(88L);

            // cover GET
            ResponseEntity<ArticleResponse> r1 = controller.get(10L);
            assertEquals(200, r1.getStatusCodeValue());

            // cover LIST dengan hasil kosong
            when(list.executeListArticles(any(ListArticles.class))).thenReturn(List.of());
            ResponseEntity<List<ArticleResponse>> r2 = controller.list(0, 10);
            assertEquals(200, r2.getStatusCodeValue());
            assertNotNull(r2.getBody());
            assertTrue(r2.getBody().isEmpty());
        }
    }
}


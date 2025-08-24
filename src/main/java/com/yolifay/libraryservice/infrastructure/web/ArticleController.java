package com.yolifay.libraryservice.infrastructure.web;

import com.yolifay.libraryservice.application.dto.article.ArticleResponse;
import com.yolifay.libraryservice.application.dto.article.CreateArticleRequest;
import com.yolifay.libraryservice.application.dto.article.UpdateArticleRequest;
import com.yolifay.libraryservice.domain.model.Article;
import com.yolifay.libraryservice.domain.usecase.article.command.*;
import com.yolifay.libraryservice.domain.usecase.article.handler.*;
import com.yolifay.libraryservice.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/articles")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ArticleController {

    private final CreateArticleHandler create;
    private final UpdateArticleHandler update;
    private final DeleteArticleHandler delete;
    private final GetArticleHandler get;
    private final ListArticlesHandler list;

    // ---- CREATE ----
    // SuperAdmin/Editor/Contributor boleh create; Viewer tidak boleh
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','EDITOR','CONTRIBUTOR')")
    @PostMapping
    public ResponseEntity<ArticleResponse> create(@Valid @RequestBody CreateArticleRequest r){
        Long userId = CurrentUser.id();
        String role = currentRole();
        log.info("[ART] CREATE req by userId={} role={} title='{}'", userId, role, r.title());

        Article saved = create.executeCreateArticle(new CreateArticle(r.title(), r.content(), userId));

        log.info("[ART] CREATE ok id={} by userId={}", saved.getId(), userId);
        return ResponseEntity.created(URI.create("/api/v1/articles/" + saved.getId()))
                .body(toResponse(saved));
    }

    // ---- UPDATE ----
    // SUPER_ADMIN boleh update apa saja.
    // EDITOR/CONTRIBUTOR hanya artikel miliknya (cek di ArticleSecurity.canEdit)
    @PreAuthorize("@artSec.canEdit(#id)")
    @PutMapping("/{id}")
    public ResponseEntity<ArticleResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateArticleRequest r){
        Long userId = CurrentUser.id();
        String role = currentRole();
        log.info("[ART] UPDATE req id={} by userId={} role={}", id, userId, role);

        update.executeUpdateArticle(new UpdateArticle(id, r.title(), r.content(), userId));
        Article updated = get.executeGetArticle(new GetArticle(id));

        log.info("[ART] UPDATE ok id={} by userId={}", id, userId);
        return ResponseEntity.ok(toResponse(updated));
    }

    // ---- DELETE ----
    // SUPER_ADMIN bisa delete apa pun; EDITOR hanya miliknya; CONTRIBUTOR/VIEWER tidak boleh
    @PreAuthorize("@artSec.canDelete(#id)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        Long userId = CurrentUser.id();
        String role = currentRole();
        log.info("[ART] DELETE req id={} by userId={} role={}", id, userId, role);

        delete.executeDeleteArticle(new DeleteArticle(id, userId));

        log.info("[ART] DELETE ok id={} by userId={}", id, userId);
        return ResponseEntity.noContent().build();
    }

    // ---- GET BY ID ----
    // Semua role boleh melihat (Viewer hanya baca).
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','EDITOR','CONTRIBUTOR','VIEWER')")
    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> get(@PathVariable Long id){
        Long userId = CurrentUser.id();
        String role = currentRole();
        log.info("[ART] GET req id={} by userId={} role={}", id, userId, role);

        Article a = get.executeGetArticle(new GetArticle(id));

        log.info("[ART] GET ok id={} by userId={}", id, userId);
        return ResponseEntity.ok(toResponse(a));
    }

    // ---- LIST ----
    // Semua role boleh melihat daftar.
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','EDITOR','CONTRIBUTOR','VIEWER')")
    @GetMapping
    public ResponseEntity<List<ArticleResponse>> list(@RequestParam(defaultValue="0") int page,
                                                      @RequestParam(defaultValue="10") int size){
        Long userId = CurrentUser.id();
        String role = currentRole();
        log.info("[ART] LIST req page={} size={} by userId={} role={}", page, size, userId, role);

        var items = list.executeListArticles(new ListArticles(page, size));
        var body = items.stream().map(ArticleController::toResponse).toList();

        log.info("[ART] LIST ok count={} for userId={}", body.size(), userId);
        return ResponseEntity.ok(body);
    }

    /** Mapper domain -> DTO */
    private static ArticleResponse toResponse(Article a) {
        return new ArticleResponse(a.getId(), a.getTitle(), a.getContent(),
                a.getAuthorId(), a.getCreatedAt(), a.getUpdatedAt());
    }

    /** Ambil role saat ini (untuk logging) */
    private String currentRole() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth == null) ? "-" :
                auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst().orElse("-");
    }
}

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
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/articles")
@RequiredArgsConstructor
@Slf4j
public class ArticleController {

    private final CreateArticleHandler create;
    private final UpdateArticleHandler update;
    private final DeleteArticleHandler delete;
    private final GetArticleHandler get;
    private final ListArticlesHandler list;


    @PostMapping
    public ResponseEntity<ArticleResponse> create(@Valid @RequestBody CreateArticleRequest r){
        log.info("Incoming Create article");
        Long userId = CurrentUser.id();
        Article saved = create.executeCreateArticle(new CreateArticle(r.title(), r.content(), userId));

        log.info("Outgoing Created article with id {}", saved.getId());
        return ResponseEntity.created(URI.create("/api/v1/articles/" + saved.getId()))
                .body(toResponse(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ArticleResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateArticleRequest r){
        log.info("Incoming Update article with id: {}", id);
        Long userId = CurrentUser.id();

        // jalankan usecase
        update.executeUpdateArticle(new UpdateArticle(id, r.title(), r.content(), userId));

        // ambil ulang dari DB untuk memastikan data benar-benar ter-update
        Article updated = get.executeGetArticle(new GetArticle(id));

        log.info("Outgoing Updated article with id {}", id);
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        log.info("Incoming Delete article");
        Long userId = CurrentUser.id();

        // jalankan usecase
        delete.executeDeleteArticle(new DeleteArticle(id, userId));

        log.info("Outgoing Deleted article with id {}", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> get(@PathVariable Long id){
        log.info("Incoming Get article");

        // jalankan usecase
        Article a = get.executeGetArticle(new GetArticle(id));

        log.info("Outgoing Got article with id {}", id);
        return ResponseEntity.ok(toResponse(a));
    }

    @GetMapping
    public ResponseEntity<List<ArticleResponse>> list(@RequestParam(defaultValue="0") int page,
                                                      @RequestParam(defaultValue="10") int size){
        log.info("Incoming List articles");
        var items = list.executeListArticles(new ListArticles(page, size));
        var body = items.stream()
                .map(ArticleController::toResponse)
                .toList();

        log.info("Outgoing Listed {} - {} articles", page, size);
        return ResponseEntity.ok(body);
    }

    /** Mapper Article (domain) -> ArticleResponse (DTO) */
    private static ArticleResponse toResponse(Article a) {
        return new ArticleResponse(
                a.getId(),
                a.getTitle(),
                a.getContent(),
                a.getAuthorId(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}

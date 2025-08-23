package com.yolifay.libraryservice.domain.port;

import com.yolifay.libraryservice.domain.model.Article;

import java.util.List;
import java.util.Optional;

public interface ArticleRepositoryPort {
    Article save(Article a);
    Optional<Article> findById(Long id);
    List<Article> findAll(int page, int size);
    void deleteById(Long id);
}

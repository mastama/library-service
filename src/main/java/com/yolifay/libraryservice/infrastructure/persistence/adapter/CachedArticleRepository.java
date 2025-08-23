package com.yolifay.libraryservice.infrastructure.persistence.adapter;

import com.yolifay.libraryservice.domain.model.Article;
import com.yolifay.libraryservice.domain.port.ArticleRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Decorator ber-cache di Redis; mendelegasikan ke JPA repo */
@Repository
@Primary
@RequiredArgsConstructor
public class CachedArticleRepository implements ArticleRepositoryPort {
    private final JpaArticleRepository delegate;

    @Override
    @CacheEvict(value = {"article_by_id","article_list"}, allEntries = true)
    public Article save(Article a){ return delegate.save(a); }

    @Override
    @Cacheable(value = "article_by_id", key = "#id")
    public Optional<Article> findById(Long id){ return delegate.findById(id); }

    @Override
    @Cacheable(value = "article_list", key = "'p'+#page+'s'+#size")
    public List<Article> findAll(int page, int size){ return delegate.findAll(page, size); }

    @Override
    @CacheEvict(value = {"article_by_id","article_list"}, allEntries = true)
    public void deleteById(Long id){ delegate.deleteById(id); }
}

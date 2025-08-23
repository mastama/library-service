package com.yolifay.libraryservice.infrastructure.persistence.adapter;

import com.yolifay.libraryservice.domain.model.Article;
import com.yolifay.libraryservice.domain.port.ArticleRepositoryPort;
import com.yolifay.libraryservice.infrastructure.persistence.mapper.ArticleMapper;
import com.yolifay.libraryservice.infrastructure.persistence.spring.SpringDataArticleRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaArticleRepository implements ArticleRepositoryPort {
    private final SpringDataArticleRepo articleRepo;

    @Override
    public Article save(Article a) {
        var saved = articleRepo.save(ArticleMapper.toArticleEntity(a));
        return ArticleMapper.toDomain(saved);
    }

    @Override
    public Optional<Article> findById(Long id) {
        return articleRepo.findById(id).map(ArticleMapper::toDomain);
    }

    @Override
    public List<Article> findAll(int page, int size) {
        return articleRepo.findAll(PageRequest.of(page, size)).map(ArticleMapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        articleRepo.deleteById(id);
    }
}

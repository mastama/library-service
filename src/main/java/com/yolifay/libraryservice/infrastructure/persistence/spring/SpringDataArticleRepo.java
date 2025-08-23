package com.yolifay.libraryservice.infrastructure.persistence.spring;

import com.yolifay.libraryservice.infrastructure.persistence.entity.ArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataArticleRepo extends JpaRepository<ArticleEntity, Long> {
}

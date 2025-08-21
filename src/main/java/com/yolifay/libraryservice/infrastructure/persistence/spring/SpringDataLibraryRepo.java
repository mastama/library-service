package com.yolifay.libraryservice.infrastructure.persistence.spring;

import com.yolifay.libraryservice.infrastructure.persistence.entity.LibraryItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataLibraryRepo extends JpaRepository<LibraryItemEntity, Long> {
}

package com.yolifay.libraryservice.infrastructure.persistence.adapter;

import com.yolifay.libraryservice.domain.model.LibraryItem;
import com.yolifay.libraryservice.domain.port.LibraryRepositoryPort;
import com.yolifay.libraryservice.infrastructure.persistence.entity.LibraryItemEntity;
import com.yolifay.libraryservice.infrastructure.persistence.mapper.LibraryMapper;
import com.yolifay.libraryservice.infrastructure.persistence.spring.SpringDataLibraryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaLibraryRepository implements LibraryRepositoryPort {
    private final SpringDataLibraryRepo repo;

    @Override
    public LibraryItem save(LibraryItem libraryItem) {
        LibraryItemEntity entity = LibraryMapper.toEntity(libraryItem);
        LibraryItemEntity savedEntity = repo.save(entity);
        return LibraryMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<LibraryItem> findById(Long id) {
        return repo.findById(id)
                .map(LibraryMapper::toDomain);
    }

    @Override
    public List<LibraryItem> findAll(int page, int size) {
        return repo.findAll(PageRequest.of(page, size))
                .stream()
                .map(LibraryMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
    }
}

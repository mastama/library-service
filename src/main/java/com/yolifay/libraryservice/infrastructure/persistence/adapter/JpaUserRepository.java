package com.yolifay.libraryservice.infrastructure.persistence.adapter;

import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.infrastructure.persistence.mapper.ArticleMapper;
import com.yolifay.libraryservice.infrastructure.persistence.mapper.UserMapper;
import com.yolifay.libraryservice.infrastructure.persistence.spring.SpringDataUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaUserRepository implements UserRepositoryPort {

    private final SpringDataUserRepo userRepo;

    @Override
    public User save(User u) {
        return UserMapper.toDomain(userRepo.save(UserMapper.toEntity(u)));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepo.findByUsername(username).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepo.findByEmail(email).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsernameOrEmail(String q) {
        return userRepo.findByUsernameOrEmail(q, q).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepo.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepo.existsByEmail(email);
    }

    @Override
    public List<User> findAll(int page, int size) {
        return userRepo.findAll(PageRequest.of(page, size)).map(UserMapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        userRepo.deleteById(id);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepo.findById(id).map(UserMapper::toDomain);
    }
}

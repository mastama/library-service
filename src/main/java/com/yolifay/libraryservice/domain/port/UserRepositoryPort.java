package com.yolifay.libraryservice.domain.port;

import com.yolifay.libraryservice.domain.model.User;

import java.util.Optional;

public interface UserRepositoryPort {
    User save(User u);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String q);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // butuh dapatkan user by id saat refresh token;
    Optional<User> findById(Long id);
}

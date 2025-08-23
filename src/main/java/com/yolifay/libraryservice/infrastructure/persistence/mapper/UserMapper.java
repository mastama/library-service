package com.yolifay.libraryservice.infrastructure.persistence.mapper;

import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.infrastructure.persistence.entity.UserEntity;

public final class UserMapper {
    private UserMapper() {}

    public static UserEntity toEntity(User u) {
        var entity = new UserEntity();
        entity.setId(u.getId());
        entity.setFullName(u.getFullName());
        entity.setUsername(u.getUsername());
        entity.setEmail(u.getEmail());
        entity.setPasswordHash(u.getPasswordHash());
        entity.setCreatedAt(u.getCreatedAt());
        return entity;
    }

    public static User toDomain(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getFullName(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getCreatedAt()
        );
    }
}

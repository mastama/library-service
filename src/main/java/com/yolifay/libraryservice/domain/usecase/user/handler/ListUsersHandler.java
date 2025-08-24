package com.yolifay.libraryservice.domain.usecase.user.handler;

import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.usecase.user.command.ListUsers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ListUsersHandler {
    private final UserRepositoryPort userRepo;

    @Cacheable(cacheNames = "users.list", key = "#c.page()+'-'+#c.size()")
    public List<User> executeListUser(ListUsers c) {
        log.info("executeListUser called");
        return userRepo.findAll(c.page(), c.size());
    }
}

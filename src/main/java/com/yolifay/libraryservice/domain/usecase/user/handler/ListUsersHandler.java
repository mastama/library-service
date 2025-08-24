package com.yolifay.libraryservice.domain.usecase.user.handler;

import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.usecase.user.command.ListUsers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ListUsersHandler {
    private final UserRepositoryPort userRepo;
    public List<User> executeListUser(ListUsers c){
        return userRepo.findAll(c.page(), c.size());
    }
}

package com.yolifay.libraryservice.domain.usecase.user.handler;

import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.usecase.user.command.DeleteUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteUserHandler {
    private final UserRepositoryPort userRepo;

    public void executeDeleteUser(DeleteUser c){
        userRepo.deleteById(c.id());
        log.info("[USER] deleted id={}", c.id());
    }
}

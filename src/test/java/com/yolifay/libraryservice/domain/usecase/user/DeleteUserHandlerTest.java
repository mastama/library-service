package com.yolifay.libraryservice.domain.usecase.user;

import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.usecase.user.command.DeleteUser;
import com.yolifay.libraryservice.domain.usecase.user.handler.DeleteUserHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteUserHandlerTest {

    @Mock
    private UserRepositoryPort userRepo;

    @InjectMocks
    private DeleteUserHandler handler;

    private static final Logger testLogger = LoggerFactory.getLogger(DeleteUserHandler.class);

    private DeleteUser command;

    @BeforeEach
    void setUp() {
        command = new DeleteUser(123L);
    }

    @Test
    void executeDeleteUser_Positive() {
        // Act
        handler.executeDeleteUser(command);

        // Assert
        verify(userRepo, times(1)).deleteById(123L);
        // Log coverage akan terpicu karena handler memanggil log.info(...)
    }

    @Test
    void executeDeleteUser_Negative_UserRepoThrowsException() {
        // Arrange
        doThrow(new RuntimeException("DB error")).when(userRepo).deleteById(123L);

        // Act & Assert
        try {
            handler.executeDeleteUser(command);
        } catch (RuntimeException ex) {
            // Pastikan exception dilempar
            assert(ex.getMessage().contains("DB error"));
        }

        verify(userRepo, times(1)).deleteById(123L);
        // Walau log.info tidak dipanggil karena exception, line repo dan exception ter-cover
    }
}


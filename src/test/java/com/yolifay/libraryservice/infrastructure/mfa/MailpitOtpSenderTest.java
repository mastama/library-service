package com.yolifay.libraryservice.infrastructure.mfa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MailpitOtpSenderTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private MailpitOtpSender otpSender;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        otpSender.from = "test@example.com"; // set value @Value
    }

    @Test
    void sendOtp_success_shouldSendEmail() {
        // Arrange
        String email = "user@example.com";
        String code = "123456";

        // Act
        otpSender.sendOtp(email, code);

        // Assert
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage sentMessage = captor.getValue();
        assertEquals("test@example.com", sentMessage.getFrom());
        assertEquals(email, Objects.requireNonNull(sentMessage.getTo())[0]);
        assertEquals("Your OTP Code", sentMessage.getSubject());
        assertTrue(Objects.requireNonNull(sentMessage.getText()).contains(code));

        // Pastikan tidak ada exception yang dilempar
    }

    @Test
    void sendOtp_fail_whenMailSenderThrowsException() {
        // Arrange
        String email = "user@example.com";
        String code = "123456";
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> otpSender.sendOtp(email, code));
        assertEquals("Failed to send OTP email", ex.getMessage());
        assertInstanceOf(RuntimeException.class, ex.getCause());
        assertEquals("SMTP error", ex.getCause().getMessage());

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}

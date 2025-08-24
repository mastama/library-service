package com.yolifay.libraryservice.infrastructure.mfa;

import com.yolifay.libraryservice.domain.service.OtpSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailpitOtpSender implements OtpSender {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@itsec.local}")
    String from;

    @Override
    public void sendOtp(String email, String code) {
        log.info("Sending OTP");
        // Here you would implement the actual email sending logic using mailSender
        // For demonstration purposes, we are just logging the action
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(email);
            message.setSubject("Your OTP Code");
            message.setText("Your OTP code is: " + code + "\nThis code will expire in 5 minutes.");
            mailSender.send(message);
            log.info("[MFA] OTP sent to email: {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
}

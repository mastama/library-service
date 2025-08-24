package com.yolifay.libraryservice.domain.usecase.auth.command;

public record OtpResponse(
        String message,      // "OTP has been sent. Please check your email."
        String delivery,     // "email"
        String to,           // "si**@example.com"
        long ttlSeconds      // mis. 300
) {
}

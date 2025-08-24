package com.yolifay.libraryservice.domain.usecase.auth.command;

public record LoginWithOtp(String usernameOrEmail, String password, String otp) {
}

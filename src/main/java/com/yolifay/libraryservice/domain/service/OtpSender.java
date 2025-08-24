package com.yolifay.libraryservice.domain.service;

public interface OtpSender {
    void sendOtp(String email, String code);
}

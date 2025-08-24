package com.yolifay.libraryservice.domain.service;

public interface LoginAttemptService {
    int onFailure(Long userId);     // return count in window
    void onSuccess(Long userId);
    boolean isBlocked(Long userId);
    long blockSecondsLeft(Long userId);
}

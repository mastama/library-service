package com.yolifay.libraryservice.infrastructure.ratelimit;

import lombok.Getter;

@Getter
public class TooManyRequestsException extends RuntimeException {
    private final long retryAfterSeconds;
    private final String ruleName;

    public TooManyRequestsException(String ruleName, long retryAfterSeconds) {
        super("Too Many Requests");
        this.ruleName = ruleName;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}

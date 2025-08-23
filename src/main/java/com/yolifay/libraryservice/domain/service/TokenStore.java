package com.yolifay.libraryservice.domain.service;

import java.time.Duration;

public interface TokenStore {
    void whitelist(String jti, Long userId, Duration ttl);
    boolean isWhitelisted(String jti);
    void revoke(String jti);
}

package com.yolifay.libraryservice.domain.service;

import java.time.Duration;

public interface RefreshTokenStore {
    //** Buat token baru utk userId, simpan di Redis dengan TTL, kembalikan token string. */
    String issue(Long userId, Duration ttl);

    /** Ambil userId dari token dan hapus token tsb (consume). return null jika tidak valid. */
    Long consume(String token);

    /** Revoke tanpa consume (opsional). */
    void revoke(String token);
}

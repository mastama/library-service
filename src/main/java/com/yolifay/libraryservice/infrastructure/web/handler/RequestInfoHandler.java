package com.yolifay.libraryservice.infrastructure.web.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.stream.Collectors;

public class RequestInfoHandler {
    private RequestInfoHandler() {}

    /** Ambil IP client (menghormati X-Forwarded-For jika ada). */
    public static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    /** Ambil userId yang disimpan di Authentication#setDetails(...) oleh JwtAuthFilter. */
    public static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        Object details = auth.getDetails();
        if (details instanceof Long l) return l;
        if (details instanceof Number n) return n.longValue();
        if (details instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
        }
        return null; // tidak ada/id tidak tersedia
    }

    /** Username dari Authentication (bisa null jika anonim). */
    public static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth == null) ? null : auth.getName();
    }

    /** Set role saat ini (mis. ROLE_SUPER_ADMIN, ROLE_EDITOR, …). */
    public static Set<String> roles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Set.of();
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Cek apakah user punya role tertentu. */
    public static boolean hasRole(String role) {
        return roles().contains(role);
    }
}

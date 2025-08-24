package com.yolifay.libraryservice.infrastructure.ratelimit;

import com.yolifay.libraryservice.domain.service.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitGuard {

    private final RateLimiter limiter;             // implementasimu: RedisRateLimiter
    private final RateLimitProperties props;       // aturan dari application.properties

    /**
     * Cek rate-limit berdasarkan rule di properties.
     * @param ruleName  nama rule (key di ratelimit.rules.*)
     * @param req       HttpServletRequest (ambil IP/xff)
     * @param userId    id user (boleh null sebelum login)
     * @param identity  identitas tambahan (mis. username/email target) — boleh null
     */
    public void check(String ruleName, HttpServletRequest req, Long userId, String identity) {
        RateLimitProperties.Rule rule = props.getRules().get(ruleName);
        if (rule == null || !props.isEnabled()) return;

        String key = buildKey(rule.getKeyBy(), req, userId, identity);
        boolean ok = limiter.allow(key, rule.getLimit(), rule.getWindow());
        if (!ok) {
            long retry = limiter.retryAfterSeconds(key, rule.getWindow());
            log.warn("[RATE-LIMIT] BLOCK rule={} key={} retryAfter={}s", ruleName, key, retry);
            throw new TooManyRequestsException(ruleName, retry);
        }
        if (log.isDebugEnabled()) {
            log.debug("[RATE-LIMIT] PASS rule={} key={}", ruleName, key);
        }
    }

    private String buildKey(RateLimitProperties.KeyBy keyBy,
                            HttpServletRequest req, Long userId, String identity) {
        String ip  = clientIp(req);
        String uid = userId == null ? "anon" : String.valueOf(userId);
        String idn = (identity == null || identity.isBlank())
                ? "" : ":" + identity.trim().toLowerCase(Locale.ROOT);

        return switch (keyBy) {
            case IP      -> "ip:" + ip + idn;
            case USER    -> "u:" + uid + idn;
            case IP_USER -> "ip:" + ip + ":u:" + uid + idn;
        };
    }

    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}

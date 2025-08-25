package com.yolifay.libraryservice.infrastructure.ratelimit;

import com.yolifay.libraryservice.domain.service.RateLimiter;
import com.yolifay.libraryservice.infrastructure.web.handler.RequestInfoHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter limiter;
    private final RateLimitProperties props;
    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        if (!props.isEnabled()) return true;

        String path = req.getRequestURI();
        String method = req.getMethod();

        for (Map.Entry<String, RateLimitProperties.Rule> e : props.getRules().entrySet()) {
            var rule = e.getValue();
            if (rule.getMethod() != null && !method.equalsIgnoreCase(rule.getMethod())) continue;
            boolean match = rule.getPaths().stream().anyMatch(p -> matcher.match(p, path));
            if (!match) continue;

            String bucketKey = buildKey(rule.getKeyBy(), req);
            boolean allowed = limiter.allow(bucketKey, rule.getLimit(), rule.getWindow());
            if (!allowed) {
                long retry = limiter.retryAfterSeconds(bucketKey, rule.getWindow());
                write429(res, retry, e.getKey(), rule);
                return false;
            }
        }
        return true;
    }

    public String clientIp(HttpServletRequest req) {
        return RequestInfoHandler.clientIp(req);
    }

    public String currentUserId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return "anon";
        Object details = a.getDetails(); // Kamu set userId di JwtAuthFilter via setDetails(userId)
        return details == null ? "anon" : String.valueOf(details);
    }

    public String buildKey(RateLimitProperties.KeyBy keyBy, HttpServletRequest req) {
        String ip = clientIp(req);
        String uid = currentUserId();
        return switch (keyBy) {
            case IP -> "ip:" + ip;
            case USER -> "u:" + uid;
            case IP_USER -> "ip:" + ip + ":u:" + uid;
        };
    }

    private void write429(HttpServletResponse res, long retryAfterSec,
                          String ruleName, RateLimitProperties.Rule rule) throws IOException {
        res.setStatus(429);
        if (retryAfterSec > 0) res.setHeader("Retry-After", String.valueOf(retryAfterSec));
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String json = """
      {"status":429,"error":"Too Many Requests",
       "rule":"%s","limit":%d,"windowSeconds":%d,"retryAfterSeconds":%d}
      """.formatted(ruleName, rule.getLimit(), rule.getWindow().toSeconds(), Math.max(0,retryAfterSec));
        res.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
    }
}

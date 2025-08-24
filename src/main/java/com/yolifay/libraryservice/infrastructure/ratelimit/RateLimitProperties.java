package com.yolifay.libraryservice.infrastructure.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitProperties {
    private boolean enabled = true;
    private Map<String, Rule> rules = new LinkedHashMap<>();

    public enum KeyBy { IP, USER, IP_USER }

    @Setter
    @Getter
    public static class Rule {
        private int limit = 60;
        private Duration window = Duration.ofMinutes(1);
        private KeyBy keyBy = KeyBy.IP;
        private List<String> paths = List.of("/**");
        private String method; // optional: GET/POST/PUT/DELETE

    }
}

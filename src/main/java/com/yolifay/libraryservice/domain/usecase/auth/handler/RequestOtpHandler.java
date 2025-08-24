package com.yolifay.libraryservice.domain.usecase.auth.handler;

import com.yolifay.libraryservice.domain.model.User;
import com.yolifay.libraryservice.domain.port.UserRepositoryPort;
import com.yolifay.libraryservice.domain.service.OtpSender;
import com.yolifay.libraryservice.domain.service.OtpStore;
import com.yolifay.libraryservice.domain.usecase.auth.command.OtpRequest;
import com.yolifay.libraryservice.infrastructure.ratelimit.RateLimitGuard;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestOtpHandler {
    private final UserRepositoryPort users;
    private final OtpSender sender;
    private final OtpStore store;

    @Value("${mfa.otp.length:6}")
    int length;
    @Value("${mfa.otp.ttl:5m}")
    Duration ttl;
    @Value("${mfa.otp.cooldown:30s}")
    Duration cooldown;

    private final RateLimitGuard rl;
    private final HttpServletRequest httpServletRequest;

    private static final SecureRandom RNG = new SecureRandom();

    public void execute(OtpRequest o){

        // batasi per IP + identity username/email
        rl.check("request-otp", httpServletRequest, null, o.usernameOrEmail());

        User u = users.findByUsernameOrEmail(o.usernameOrEmail().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String key = "login:" + u.getId();

        // anti-spam sederhana: jika masih ada OTP aktif, jangan generate baru
        if (store.get(key) != null) {
            log.info("[MFA] OTP still valid for userId={} (cooldown active)", u.getId());
            return;
        }

        String code = gen(length);
        store.put(key, code, ttl);
        sender.sendOtp(u.getEmail(), code);
        log.info("[MFA] OTP generated for userId={}, ttl={}s", u.getId(), ttl.getSeconds());
    }

    private String gen(int len){
        StringBuilder sb = new StringBuilder(len);
        for(int i=0;i<len;i++) sb.append(RNG.nextInt(10));
        return sb.toString();
    }
}

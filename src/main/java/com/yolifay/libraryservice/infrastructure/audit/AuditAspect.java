package com.yolifay.libraryservice.infrastructure.audit;

import com.yolifay.libraryservice.domain.model.AuditEvent;
import com.yolifay.libraryservice.domain.port.AuditLogRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogRepositoryPort auditRepo;

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        var request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String ua = request.getHeader("User-Agent");
        String ip = Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                .orElseGet(request::getRemoteAddr);

        String resource = request.getRequestURI();
        String method = request.getMethod();
        Long actorId = com.yolifay.libraryservice.infrastructure.security.CurrentUser.id();

        Instant start = Instant.now();
        try {
            Object result = pjp.proceed();
            int status = 200;
            auditRepo.save(new AuditEvent(null, actorId, audited.action(), resource, method, ua, ip, status, start));
            log.info("[AUDIT] action={} resource={} userId={} ip={} ua={}", audited.action(), resource, actorId, ip, ua);
            return result;
        } catch (Throwable ex) {
            auditRepo.save(new AuditEvent(null, actorId, audited.action(), resource, method, ua, ip, 500, start));
            log.warn("[AUDIT] action={} FAILED userId={} err={}", audited.action(), actorId, ex.toString());
            throw ex;
        }
    }
}

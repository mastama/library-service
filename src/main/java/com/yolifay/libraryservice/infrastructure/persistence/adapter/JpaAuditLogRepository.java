package com.yolifay.libraryservice.infrastructure.persistence.adapter;

import com.yolifay.libraryservice.domain.model.AuditEvent;
import com.yolifay.libraryservice.domain.port.AuditLogRepositoryPort;
import com.yolifay.libraryservice.infrastructure.persistence.entity.AuditEventEntity;
import com.yolifay.libraryservice.infrastructure.persistence.spring.SpringDataAuditRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository @RequiredArgsConstructor
public class JpaAuditLogRepository implements AuditLogRepositoryPort {

    private final SpringDataAuditRepo repo;

    @Override public AuditEvent save(AuditEvent e){
        var en = new AuditEventEntity();
        en.setActorId(e.getActorId()); en.setAction(e.getAction());
        en.setResource(e.getResource()); en.setMethod(e.getMethod());
        en.setUserAgent(e.getUserAgent()); en.setIp(e.getIp());
        en.setStatus(e.getStatus()); en.setAt(e.getAt());
        var saved = repo.save(en);
        return new AuditEvent(saved.getId(), saved.getActorId(), saved.getAction(),
                saved.getResource(), saved.getMethod(), saved.getUserAgent(),
                saved.getIp(), saved.getStatus(), saved.getAt());
    }
    @Override public List<AuditEvent> findAll(int page, int size){
        return repo.findAll(PageRequest.of(page,size)).map(e ->
                new AuditEvent(e.getId(), e.getActorId(), e.getAction(), e.getResource(),
                        e.getMethod(), e.getUserAgent(), e.getIp(), e.getStatus(), e.getAt())
        ).toList();
    }
}

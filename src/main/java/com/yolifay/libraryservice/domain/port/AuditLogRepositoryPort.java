package com.yolifay.libraryservice.domain.port;

import com.yolifay.libraryservice.domain.model.AuditEvent;

import java.util.List;

public interface AuditLogRepositoryPort {
    AuditEvent save(AuditEvent auditEvent);
    List<AuditEvent> findAll(int page, int size);
}

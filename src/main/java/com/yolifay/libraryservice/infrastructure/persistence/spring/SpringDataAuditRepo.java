package com.yolifay.libraryservice.infrastructure.persistence.spring;

import com.yolifay.libraryservice.infrastructure.persistence.entity.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAuditRepo extends JpaRepository<AuditEventEntity, Long> {
}

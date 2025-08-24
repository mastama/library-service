package com.yolifay.libraryservice.infrastructure.web;

import com.yolifay.libraryservice.domain.model.AuditEvent;
import com.yolifay.libraryservice.domain.port.AuditLogRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepositoryPort auditRepo;

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<List<AuditEvent>> list(@RequestParam(defaultValue="0") int page,
                                                 @RequestParam(defaultValue="20") int size){
        return ResponseEntity.ok(auditRepo.findAll(page, size));
    }
}

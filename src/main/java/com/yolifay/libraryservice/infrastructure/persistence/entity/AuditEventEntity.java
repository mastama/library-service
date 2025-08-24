package com.yolifay.libraryservice.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name="audit_events", indexes=@Index(name="idx_audit_at", columnList="at"))
@Getter
@Setter
public class AuditEventEntity {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private Long actorId;
    private String action;
    private String resource;
    private String method;
    @Column(length=512) private String userAgent;
    private String ip;
    private int status;
    private Instant at;
}

package com.yolifay.libraryservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class AuditEvent {
    private final Long id;
    private final Long actorId;          // boleh null (anonymous)
    private final String action;         // LOGIN, CREATE_ARTICLE, UPDATE_ARTICLE, ...
    private final String resource;       // /api/v1/articles
    private final String method;         // GET/POST/PUT/DELETE
    private final String userAgent;
    private final String ip;
    private final int status;            // HTTP status
    private final Instant at;
}

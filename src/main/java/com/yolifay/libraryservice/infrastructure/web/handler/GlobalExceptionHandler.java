package com.yolifay.libraryservice.infrastructure.web.handler;

import com.yolifay.libraryservice.infrastructure.ratelimit.TooManyRequestsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // === 429: Rate limit ===
    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<Map<String,Object>> tooMany(TooManyRequestsException ex){
        long retry = Math.max(1, ex.getRetryAfterSeconds());
        log.warn("429 TooManyRequests rule={} retryAfter={}s", ex.getRuleName(), retry);
        return ResponseEntity.status(429)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(retry))
                .body(Map.of(
                        "status", 429,
                        "error", "Too Many Requests",
                        "rule", ex.getRuleName(),
                        "retryAfterSeconds", retry
                ));
    }

    // === 400: Validasi payload ===
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> badRequest(MethodArgumentNotValidException ex){
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, DefaultMessageSourceResolvable::getDefaultMessage, (a, b)->a));
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", "Validation failed",
                "fields", errors
        ));
    }

    // === 400: Salah tipe parameter/path variable ===
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String,Object>> badType(MethodArgumentTypeMismatchException ex){
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", "Invalid parameter: " + ex.getName()
        ));
    }

    // === 400 default untuk IllegalArgumentException (opsional)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,Object>> illegalArg(IllegalArgumentException ex){
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage()
        ));
    }
}

package com.yolifay.libraryservice.infrastructure.web.handler;

import com.yolifay.libraryservice.infrastructure.ratelimit.TooManyRequestsException;
import com.yolifay.libraryservice.infrastructure.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // ========== TooManyRequests (429) ==========

    @Test
    void tooMany_withPositiveRetry_setsHeaderAndBody() {
        TooManyRequestsException ex = mock(TooManyRequestsException.class);
        when(ex.getRetryAfterSeconds()).thenReturn(12L);
        when(ex.getRuleName()).thenReturn("login");

        ResponseEntity<Map<String,Object>> resp = handler.tooMany(ex);

        assertEquals(429, resp.getStatusCodeValue());
        assertEquals("12", resp.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));

        Map<String,Object> body = resp.getBody();
        assertNotNull(body);
        assertEquals(429, body.get("status"));
        assertEquals("Too Many Requests", body.get("error"));
        assertEquals("login", body.get("rule"));
        assertEquals(12L, body.get("retryAfterSeconds"));
    }

    @Test
    void tooMany_withZeroOrNegativeRetry_forcesMinimumOneSecond() {
        TooManyRequestsException ex = mock(TooManyRequestsException.class);
        when(ex.getRetryAfterSeconds()).thenReturn(0L); // atau -5L
        when(ex.getRuleName()).thenReturn("otp");

        ResponseEntity<Map<String,Object>> resp = handler.tooMany(ex);

        assertEquals(429, resp.getStatusCodeValue());
        assertEquals("1", resp.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)); // Math.max(1, ...)
        assertEquals(1L, resp.getBody().get("retryAfterSeconds"));
    }

    // ========== Bad Request (validation) ==========

//    @Test
//    void badRequest_withDuplicateFields_andNullMessage_mergesAndKeepsFirst() {
//        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
//        BindingResult br = mock(BindingResult.class);
//        when(ex.getBindingResult()).thenReturn(br);
//
//        FieldError e1 = new FieldError("user", "email", "invalid");            // pertama
//        FieldError e2 = new FieldError("user", "email", "required");           // duplikat -> diabaikan
//        FieldError e3 = new FieldError("user", "username", (String) null);     // null message
//        when(br.getFieldErrors()).thenReturn(List.of(e1, e2, e3));
//
//        ResponseEntity<Map<String,Object>> resp = handler.badRequest(ex);
//
//        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
//
//        Map<String,Object> body = resp.getBody();
//        assertNotNull(body);
//        assertEquals(400, body.get("status"));
//        assertEquals("Bad Request", body.get("error"));
//        assertEquals("Validation failed", body.get("message"));
//
//        @SuppressWarnings("unchecked")
//        Map<String,Object> fields = (Map<String, Object>) body.get("fields");
//        assertEquals(2, fields.size());
//        assertEquals("invalid", fields.get("email"));   // pesan pertama dipertahankan
//        assertTrue(fields.containsKey("username"));
//        assertNull(fields.get("username"));             // null value diperbolehkan di inner map
//    }

    @Test
    void badRequest_withNoFieldErrors_returnsEmptyFieldsMap() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(br);
        when(br.getFieldErrors()).thenReturn(List.of());

        ResponseEntity<Map<String,Object>> resp = handler.badRequest(ex);

        assertEquals(400, resp.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String,Object> fields = (Map<String, Object>) resp.getBody().get("fields");
        assertNotNull(fields);
        assertTrue(fields.isEmpty());
    }

    // ========== Bad type (400) ==========

    @Test
    void badType_includesParameterNameInMessage() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("page");

        ResponseEntity<Map<String,Object>> resp = handler.badType(ex);

        assertEquals(400, resp.getStatusCodeValue());
        assertEquals("Bad Request", resp.getBody().get("error"));
        assertEquals("Invalid parameter: page", resp.getBody().get("message"));
    }

    @Test
    void badType_whenNameNull_stillBuildsStringWithNullWord() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn(null);

        ResponseEntity<Map<String,Object>> resp = handler.badType(ex);

        assertEquals(400, resp.getStatusCodeValue());
        assertEquals("Invalid parameter: null", resp.getBody().get("message"));
    }

    // ========== IllegalArgumentException default (400) ==========

    @Test
    void illegalArg_withMessage_returnsBadRequestBody() {
        IllegalArgumentException ex = new IllegalArgumentException("bad_input");

        ResponseEntity<Map<String,Object>> resp = handler.illegalArg(ex);

        assertEquals(400, resp.getStatusCodeValue());
        assertEquals("Bad Request", resp.getBody().get("error"));
        assertEquals("bad_input", resp.getBody().get("message"));
    }

    @Test
    void illegalArg_withNullMessage_throwsNullPointerBecauseMapOfDisallowsNull() {
        IllegalArgumentException ex = new IllegalArgumentException(); // message == null

        assertThrows(NullPointerException.class, () -> handler.illegalArg(ex));
    }
}


package com.yolifay.libraryservice.infrastructure;

import com.yolifay.libraryservice.infrastructure.web.RestExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    // ========== @ExceptionHandler(MethodArgumentNotValidException) ==========

    @Test
    void validationError_withDuplicateFieldErrors_keepsFirstMessage_andReturns400() {
        // arrange
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(br);

        // Dua error untuk field "email" → merge function (a,b)->a menjaga pesan pertama ("invalid")
        FieldError e1 = new FieldError("user", "email", "invalid");
        FieldError e2 = new FieldError("user", "email", "required");
        FieldError e3 = new FieldError("user", "username", "must not be blank");
        when(br.getFieldErrors()).thenReturn(List.of(e1, e2, e3));

        // act
        ResponseEntity<?> resp = handler.validationError(ex);

        // assert
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertTrue(resp.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) resp.getBody();
        assertEquals("validation_error", body.get("message"));

        assertTrue(body.get("errors") instanceof Map);
        Map<?, ?> errors = (Map<?, ?>) body.get("errors");
        assertEquals(2, errors.size());
        assertEquals("invalid", errors.get("email"));          // pesan pertama dipertahankan
        assertEquals("must not be blank", errors.get("username"));
    }

    @Test
    void validationError_withEmptyErrors_returnsEmptyErrorsMap_and400() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(br);
        when(br.getFieldErrors()).thenReturn(List.of()); // kosong

        ResponseEntity<?> resp = handler.validationError(ex);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) resp.getBody();
        Map<?, ?> errors = (Map<?, ?>) body.get("errors");
        assertNotNull(errors);
        assertTrue(errors.isEmpty());
    }

//    @Test
//    void validationError_withNullDefaultMessage_allowsNullValueInInnerMap() {
//        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
//        BindingResult br = mock(BindingResult.class);
//        when(ex.getBindingResult()).thenReturn(br);
//
//        // defaultMessage = null → HashMap dari toMap boleh menyimpan null values
//        FieldError e = new FieldError("user", "phone", null);
//        when(br.getFieldErrors()).thenReturn(List.of(e));
//
//        ResponseEntity<?> resp = handler.validationError(ex);
//
//        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
//        Map<?, ?> errors = (Map<?, ?>) ((Map<?, ?>) Objects.requireNonNull(resp.getBody())).get("errors");
//        assertTrue(errors.containsKey("phone"));
//        assertNull(errors.get("phone")); // nilai null di inner map valid
//    }

    // ========== @ExceptionHandler(IllegalArgumentException) ==========

    @Test
    void notFound_withMessage_returns404_andBodyMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("user_not_found");

        ResponseEntity<?> resp = handler.notFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) resp.getBody();
        assertEquals("user_not_found", body.get("message"));
    }

    @Test
    void notFound_withNullMessage_throwsNullPointerException_dueToMapOf() {
        // new IllegalArgumentException() → message == null
        IllegalArgumentException ex = new IllegalArgumentException();

        assertThrows(NullPointerException.class, () -> handler.notFound(ex));
        // Catatan: Map.of tidak menerima nilai null → ini mengekspos perilaku negatif saat message null.
    }
}

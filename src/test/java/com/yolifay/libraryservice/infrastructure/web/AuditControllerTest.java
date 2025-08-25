package com.yolifay.libraryservice.infrastructure.web;

import com.yolifay.libraryservice.domain.model.AuditEvent;
import com.yolifay.libraryservice.domain.port.AuditLogRepositoryPort;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    private final AuditLogRepositoryPort auditRepo = mock(AuditLogRepositoryPort.class);
    private final AuditController controller = new AuditController(auditRepo);

    // ====== POSITIVE ======

    @Test
    void list_success_returnsOk_andPassesPagingToRepo() {
        // arrange
        AuditEvent e1 = mock(AuditEvent.class);
        AuditEvent e2 = mock(AuditEvent.class);
        List<AuditEvent> events = List.of(e1, e2);
        when(auditRepo.findAll(1, 5)).thenReturn(events);

        // act
        ResponseEntity<List<AuditEvent>> resp = controller.list(1, 5);

        // assert
        assertEquals(200, resp.getStatusCodeValue());
        assertSame(events, resp.getBody()); // body adalah list yang sama dari repo
        verify(auditRepo, times(1)).findAll(1, 5);
        verifyNoMoreInteractions(auditRepo);
    }

    @Test
    void list_success_emptyList_ok() {
        // arrange
        when(auditRepo.findAll(0, 20)).thenReturn(List.of());

        // act
        ResponseEntity<List<AuditEvent>> resp = controller.list(0, 20);

        // assert
        assertEquals(200, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isEmpty());
        verify(auditRepo).findAll(0, 20);
    }

    // ====== NEGATIVE ======
    // Controller tidak punya validasi; jadi "negatif" di sini memverifikasi
    // propagasi error dari repository (malformed input / kegagalan internal).

    @Test
    void list_whenRepoThrowsIllegalArgumentException_isPropagated() {
        when(auditRepo.findAll(-1, 10)).thenThrow(new IllegalArgumentException("page < 0"));

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> controller.list(-1, 10));
        assertEquals("page < 0", ex.getMessage());
        verify(auditRepo).findAll(-1, 10);
    }

    @Test
    void list_whenRepoThrowsRuntimeException_isPropagated() {
        when(auditRepo.findAll(0, 20)).thenThrow(new RuntimeException("boom"));

        RuntimeException ex =
                assertThrows(RuntimeException.class, () -> controller.list(0, 20));
        assertEquals("boom", ex.getMessage());
        verify(auditRepo).findAll(0, 20);
    }
}


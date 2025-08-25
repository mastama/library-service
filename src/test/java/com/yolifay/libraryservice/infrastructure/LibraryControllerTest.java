package com.yolifay.libraryservice.infrastructure;

import com.yolifay.libraryservice.application.dto.CreateLibraryRequest;
import com.yolifay.libraryservice.application.dto.IdResponse;
import com.yolifay.libraryservice.application.dto.LibraryResponse;
import com.yolifay.libraryservice.application.dto.UpdateLibraryRequest;
import com.yolifay.libraryservice.domain.model.LibraryItem;

import com.yolifay.libraryservice.domain.usecase.library.command.*;
import com.yolifay.libraryservice.domain.usecase.library.handler.*;
import com.yolifay.libraryservice.infrastructure.web.LibraryController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryControllerTest {

    private final CreateLibraryHandler createHandler = mock(CreateLibraryHandler.class);
    private final UpdateLibraryHandler updateHandler = mock(UpdateLibraryHandler.class);
    private final DeleteLibraryHandler deleteHandler = mock(DeleteLibraryHandler.class);
    private final GetLibraryHandler getHandler = mock(GetLibraryHandler.class);
    private final ListLibraryHandler listHandler = mock(ListLibraryHandler.class);

    private LibraryController controller;

    @BeforeEach
    void setUp() {
        controller = new LibraryController(createHandler, updateHandler, deleteHandler, getHandler, listHandler);
    }

    // ---------- Helper untuk akses record/POJO field via refleksi ----------
    private static Object call(Object obj, String... methods) {
        for (String m : methods) {
            try {
                Method md = obj.getClass().getMethod(m);
                return md.invoke(obj);
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                fail("Accessor invocation failed: " + e.getMessage());
            }
        }
        fail("No accessor found on type " + obj.getClass().getName());
        return null; // unreachable
    }

    private static Long respId(Object resp) {
        return (Long) call(resp, "id", "getId");
    }

    // ===================== CREATE =====================

    @Test
    void createLibrary_success_mapsAndReturnsId() {
        CreateLibraryRequest req = mock(CreateLibraryRequest.class);
        when(req.title()).thenReturn("T");
        when(req.content()).thenReturn("C");
        when(req.authorId()).thenReturn(9L);

        when(createHandler.execute(any(CreateLibrary.class))).thenReturn(42L);

        IdResponse out = controller.createLibrary(req);

        assertNotNull(out);
        assertEquals(42L, respId(out));
        verify(createHandler).execute(argThat(c ->
                c.title().equals("T") && c.content().equals("C") && c.authorId().equals(9L)
        ));
    }

    @Test
    void createLibrary_whenHandlerThrows_isPropagated() {
        CreateLibraryRequest req = mock(CreateLibraryRequest.class);
        when(req.title()).thenReturn("X");
        when(req.content()).thenReturn("Y");
        when(req.authorId()).thenReturn(1L);

        when(createHandler.execute(any(CreateLibrary.class)))
                .thenThrow(new RuntimeException("boom"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> controller.createLibrary(req));
        assertEquals("boom", ex.getMessage());
    }

    // ===================== UPDATE =====================

    @Test
    void updateLibrary_success_callsHandlerWithPayload() {
        UpdateLibraryRequest req = mock(UpdateLibraryRequest.class);
        when(req.title()).thenReturn("NT");
        when(req.content()).thenReturn("NC");

        doNothing().when(updateHandler).execute(any(UpdateLibrary.class));

        controller.updateLibrary(77L, req);

        verify(updateHandler).execute(argThat(u ->
                u.id().equals(77L) && u.title().equals("NT") && u.content().equals("NC")
        ));
    }

    @Test
    void updateLibrary_whenHandlerThrows_isPropagated() {
        UpdateLibraryRequest req = mock(UpdateLibraryRequest.class);
        when(req.title()).thenReturn("A");
        when(req.content()).thenReturn("B");

        doThrow(new IllegalStateException("fail")).when(updateHandler).execute(any(UpdateLibrary.class));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> controller.updateLibrary(5L, req));
        assertEquals("fail", ex.getMessage());
    }

    // ===================== DELETE =====================

    @Test
    void deleteLibrary_success_callsHandler() {
        doNothing().when(deleteHandler).execute(any(DeleteLibrary.class));

        controller.deleteLibrary(88L);

        verify(deleteHandler).execute(argThat(d -> d.id().equals(88L)));
    }

    @Test
    void deleteLibrary_whenHandlerThrows_isPropagated() {
        doThrow(new RuntimeException("d-fail")).when(deleteHandler).execute(any(DeleteLibrary.class));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> controller.deleteLibrary(9L));
        assertEquals("d-fail", ex.getMessage());
    }

    // ===================== GET =====================

    @Test
    void getLibrary_success_mapsItemToResponse() {
        LibraryItem item = mock(LibraryItem.class);
        when(item.getId()).thenReturn(3L);
        when(item.getTitle()).thenReturn("T");
        when(item.getContent()).thenReturn("C");
        when(item.getAuthorId()).thenReturn(7L);
        Instant now = Instant.now();
        when(item.getCreatedAt()).thenReturn(now);
        when(item.getUpdatedAt()).thenReturn(now);

        when(getHandler.execute(any(GetLibrary.class))).thenReturn(item);

        LibraryResponse resp = controller.getLibrary(3L);

        assertNotNull(resp);
        assertEquals(3L, respId(resp)); // cek id via refleksi (record/POJO)
        verify(getHandler).execute(argThat(g -> g.id().equals(3L)));
        // Baris log "Outgoing ..." & konstruktor LibraryResponse ikut dieksekusi
    }

    @Test
    void getLibrary_whenHandlerThrows_isPropagated() {
        when(getHandler.execute(any(GetLibrary.class))).thenThrow(new RuntimeException("g-fail"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> controller.getLibrary(1L));
        assertEquals("g-fail", ex.getMessage());
    }

    // ===================== LIST =====================

    @Test
    void listLibraries_success_nonEmpty_mapsAllItems() {
        LibraryItem a = mock(LibraryItem.class);
        when(a.getId()).thenReturn(1L);
        when(a.getTitle()).thenReturn("A");
        when(a.getContent()).thenReturn("CA");
        when(a.getAuthorId()).thenReturn(10L);
        Instant now = Instant.now();
        when(a.getCreatedAt()).thenReturn(now);
        when(a.getUpdatedAt()).thenReturn(now);

        LibraryItem b = mock(LibraryItem.class);
        when(b.getId()).thenReturn(2L);
        when(b.getTitle()).thenReturn("B");
        when(b.getContent()).thenReturn("CB");
        when(b.getAuthorId()).thenReturn(11L);
        when(b.getCreatedAt()).thenReturn(now);
        when(b.getUpdatedAt()).thenReturn(now);

        when(listHandler.execute(any(ListLibrary.class))).thenReturn(List.of(a, b));

        List<LibraryResponse> out = controller.listLibraries(0, 10);

        assertNotNull(out);
        assertEquals(2, out.size());
        assertEquals(1L, respId(out.get(0)));
        assertEquals(2L, respId(out.get(1)));
        verify(listHandler).execute(argThat(l -> l.page() == 0 && l.size() == 10));
        // mapping lambda .map(...) dan .toList() dieksekusi untuk kedua elemen
    }

    @Test
    void listLibraries_success_empty_returnsEmptyList() {
        when(listHandler.execute(any(ListLibrary.class))).thenReturn(List.of());

        List<LibraryResponse> out = controller.listLibraries(3, 5);

        assertNotNull(out);
        assertTrue(out.isEmpty());
        verify(listHandler).execute(argThat(l -> l.page() == 3 && l.size() == 5));
    }

    @Test
    void listLibraries_whenHandlerThrows_isPropagated() {
        when(listHandler.execute(any(ListLibrary.class))).thenThrow(new RuntimeException("l-fail"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> controller.listLibraries(1, 1));
        assertEquals("l-fail", ex.getMessage());
    }
}

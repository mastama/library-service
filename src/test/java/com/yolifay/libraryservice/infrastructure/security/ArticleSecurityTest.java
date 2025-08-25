package com.yolifay.libraryservice.infrastructure.security;

import com.yolifay.libraryservice.domain.model.Article;
import com.yolifay.libraryservice.domain.port.ArticleRepositoryPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleSecurityTest {

    private final ArticleRepositoryPort repo = mock(ArticleRepositoryPort.class);
    private ArticleSecurity sec;

    @BeforeEach
    void setUp() {
        sec = new ArticleSecurity(repo);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---------- helpers ----------
    private void setAuth(String role, Object details) {
        var auth = new UsernamePasswordAuthenticationToken(
                "user", "pwd",
                role == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        auth.setDetails(details); // Long atau bukan Long
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static Article articleWithAuthor(long authorId) {
        Instant t = Instant.parse("2025-01-01T00:00:00Z");
        return new Article(1L, "t", "c", authorId, t, t);
    }

    // ================= canEdit =================

    @Test
    void canEdit_superAdmin_true_evenIfNotOwner() {
        setAuth("SUPER_ADMIN", 5L);
        when(repo.findById(10L)).thenReturn(Optional.of(articleWithAuthor(999L)));

        assertTrue(sec.canEdit(10L));
        verify(repo).findById(10L);
    }

    @Test
    void canEdit_editor_owner_true() {
        setAuth("EDITOR", 7L);
        when(repo.findById(33L)).thenReturn(Optional.of(articleWithAuthor(7L)));

        assertTrue(sec.canEdit(33L));
        verify(repo).findById(33L);
    }

    @Test
    void canEdit_editor_notOwner_false_optionalEmpty() {
        setAuth("EDITOR", 7L);
        when(repo.findById(44L)).thenReturn(Optional.empty()); // owner=false via orElse(false)

        assertFalse(sec.canEdit(44L));
        verify(repo).findById(44L);
    }

    @Test
    void canEdit_contributor_owner_true() {
        setAuth("CONTRIBUTOR", 21L);
        when(repo.findById(50L)).thenReturn(Optional.of(articleWithAuthor(21L)));

        assertTrue(sec.canEdit(50L));
        verify(repo).findById(50L);
    }

    @Test
    void canEdit_viewer_ownerTrue_butStillFalse() {
        setAuth("VIEWER", 9L);
        when(repo.findById(60L)).thenReturn(Optional.of(articleWithAuthor(9L)));

        assertFalse(sec.canEdit(60L));
        verify(repo).findById(60L);
    }

    @Test
    void canEdit_detailsNotLong_roleEditor_false() {
        setAuth("EDITOR", "not-long"); // uid=null
        when(repo.findById(70L)).thenReturn(Optional.of(articleWithAuthor(70L)));

        assertFalse(sec.canEdit(70L));
        verify(repo).findById(70L);
    }

    @Test
    void canEdit_authNull_false() {
        SecurityContextHolder.clearContext();
        when(repo.findById(80L)).thenReturn(Optional.of(articleWithAuthor(80L)));

        assertFalse(sec.canEdit(80L));
        verify(repo).findById(80L);
    }

    @Test
    void canEdit_emptyAuthorities_false_evenIfOwner() {
        setAuth(null, 5L); // role "-"
        when(repo.findById(81L)).thenReturn(Optional.of(articleWithAuthor(5L)));

        assertFalse(sec.canEdit(81L));
        verify(repo).findById(81L);
    }

    // ================= canDelete =================

    @Test
    void canDelete_superAdmin_true_evenIfNotOwner() {
        setAuth("SUPER_ADMIN", 123L);
        when(repo.findById(1L)).thenReturn(Optional.of(articleWithAuthor(999L)));

        assertTrue(sec.canDelete(1L));
        verify(repo).findById(1L);
    }

    @Test
    void canDelete_editor_owner_true() {
        setAuth("EDITOR", 42L);
        when(repo.findById(2L)).thenReturn(Optional.of(articleWithAuthor(42L)));

        assertTrue(sec.canDelete(2L));
        verify(repo).findById(2L);
    }

    @Test
    void canDelete_editor_notOwner_false() {
        setAuth("EDITOR", 42L);
        when(repo.findById(3L)).thenReturn(Optional.of(articleWithAuthor(777L)));

        assertFalse(sec.canDelete(3L));
        verify(repo).findById(3L);
    }

    @Test
    void canDelete_authNull_false_evenIfOwner() {
        SecurityContextHolder.clearContext();
        when(repo.findById(4L)).thenReturn(Optional.of(articleWithAuthor(4L)));

        assertFalse(sec.canDelete(4L));
        verify(repo).findById(4L);
    }

    @Test
    void canDelete_emptyAuthorities_false() {
        setAuth(null, 9L);
        when(repo.findById(5L)).thenReturn(Optional.of(articleWithAuthor(9L)));

        assertFalse(sec.canDelete(5L));
        verify(repo).findById(5L);
    }
}


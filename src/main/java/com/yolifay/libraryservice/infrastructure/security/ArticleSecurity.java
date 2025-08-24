package com.yolifay.libraryservice.infrastructure.security;

import com.yolifay.libraryservice.domain.model.Article;
import com.yolifay.libraryservice.domain.port.ArticleRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component("artSec")        // <-- nama bean untuk dipakai di SpEL @PreAuthorize
@RequiredArgsConstructor
public class ArticleSecurity {

    private final ArticleRepositoryPort articles;

    public boolean canEdit(Long articleId) {
        Auth a = auth();
        boolean owner = isOwner(articleId, a.userId());
        boolean ok = a.hasRole("SUPER_ADMIN") || ((a.hasRole("EDITOR") || a.hasRole("CONTRIBUTOR")) && owner);

        log.info("[RBAC] canEdit? userId={} role={} articleId={} owner={} -> {}",
                a.userId(), a.role(), articleId, owner, ok);
        return ok;
    }

    public boolean canDelete(Long articleId) {
        Auth a = auth();
        boolean owner = isOwner(articleId, a.userId());
        // SUPER_ADMIN bisa delete apa pun; EDITOR hanya miliknya; CONTRIBUTOR/VIEWER tidak boleh
        boolean ok = a.hasRole("SUPER_ADMIN") || (a.hasRole("EDITOR") && owner);

        log.info("[RBAC] canDelete? userId={} role={} articleId={} owner={} -> {}",
                a.userId(), a.role(), articleId, owner, ok);
        return ok;
    }

    private boolean isOwner(Long articleId, Long userId) {
        return articles.findById(articleId)
                .map(Article::getAuthorId)
                .map(uid -> uid.equals(userId))
                .orElse(false);
    }

    // -------- helpers ----------
    private Auth auth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long uid = (auth != null && auth.getDetails() instanceof Long l) ? l : null;
        String role = (auth == null) ? "-" :
                auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst().orElse("-");
        return new Auth(uid, role);
    }

    private record Auth(Long userId, String springRole) {
        boolean hasRole(String r){ return ("ROLE_" + r).equals(springRole); }
        String role(){ return springRole; }
    }
}

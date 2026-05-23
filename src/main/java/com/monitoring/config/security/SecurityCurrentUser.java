package com.monitoring.config.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Текущий пользователь из JWT-контекста.
 */
@Component
public class SecurityCurrentUser {

    public String username() {
        return requireAuth().getName();
    }

    public boolean isAdmin() {
        var auth = requireAuth();
        if ("admin".equalsIgnoreCase(auth.getName())) {
            return true;
        }
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ADMIN".equals(a));
    }

    private static Authentication requireAuth() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Требуется аутентификация");
        }
        var name = auth.getName();
        if (name == null || "anonymousUser".equals(name)) {
            throw new AccessDeniedException("Требуется аутентификация");
        }
        return auth;
    }
}

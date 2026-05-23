package com.monitoring.core.application.ports.out.security;

/**
 * Выходной порт выпуска JWT после успешной аутентификации.
 */
public interface TokenIssuer {

    String issue(String username, String role);
}

package com.monitoring.core.application.ports.out.security;

/**
 * Выходной порт проверки пароля без привязки к конкретной библиотеке.
 */
public interface PasswordVerifier {

    boolean matches(String rawPassword, String encodedHash);
}

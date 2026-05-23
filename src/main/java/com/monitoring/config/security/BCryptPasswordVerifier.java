package com.monitoring.config.security;

import com.monitoring.core.application.ports.out.security.PasswordVerifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Проверка пароля через стандартный {@link PasswordEncoder} (BCrypt).
 */
@Component
public class BCryptPasswordVerifier implements PasswordVerifier {

    private final PasswordEncoder passwordEncoder;

    public BCryptPasswordVerifier(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean matches(String rawPassword, String encodedHash) {
        return passwordEncoder.matches(rawPassword, encodedHash);
    }
}

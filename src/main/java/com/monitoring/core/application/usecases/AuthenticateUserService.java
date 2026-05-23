package com.monitoring.core.application.usecases;

import com.monitoring.core.application.ports.in.AuthenticateUserUseCase;
import com.monitoring.core.application.ports.out.repositories.EngineerRepository;
import com.monitoring.core.application.ports.out.security.PasswordVerifier;
import com.monitoring.core.application.ports.out.security.TokenIssuer;

import java.util.Optional;

/**
 * Сценарий аутентификации: загрузка инженера, проверка пароля, выпуск токена.
 */
public class AuthenticateUserService implements AuthenticateUserUseCase {

    private final EngineerRepository engineers;
    private final PasswordVerifier passwordVerifier;
    private final TokenIssuer tokenIssuer;

    public AuthenticateUserService(
            EngineerRepository engineers,
            PasswordVerifier passwordVerifier,
            TokenIssuer tokenIssuer
    ) {
        this.engineers = engineers;
        this.passwordVerifier = passwordVerifier;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    public Optional<String> authenticate(String username, String rawPassword) {
        return engineers.findByUsername(username)
                .filter(e -> passwordVerifier.matches(rawPassword, e.passwordHash()))
                .map(e -> tokenIssuer.issue(e.username(), e.role()));
    }
}

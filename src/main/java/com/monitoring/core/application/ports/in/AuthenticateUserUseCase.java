package com.monitoring.core.application.ports.in;

import java.util.Optional;

/**
 * Входной порт аутентификации по логину и паролю.
 */
public interface AuthenticateUserUseCase {

    /**
     * @return JWT-токен при успешной проверке учётных данных
     */
    Optional<String> authenticate(String username, String rawPassword);
}

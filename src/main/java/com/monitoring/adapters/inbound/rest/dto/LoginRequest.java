package com.monitoring.adapters.inbound.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Запрос аутентификации по логину и паролю.
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}

package com.monitoring.core.domain;

import java.util.Objects;

/**
 * Учётная запись инженера или администратора. Роль хранится строкой, чтобы совпадать с CHECK-ограничением БД.
 */
public record Engineer(
        Long id,
        String username,
        String passwordHash,
        String role,
        String phone,
        String notificationPrefs
) {
    public Engineer {
        Objects.requireNonNull(username, "username обязателен");
        Objects.requireNonNull(passwordHash, "passwordHash обязателен");
        Objects.requireNonNull(role, "role обязателен");
    }
}

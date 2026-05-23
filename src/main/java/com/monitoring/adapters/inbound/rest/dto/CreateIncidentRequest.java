package com.monitoring.adapters.inbound.rest.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Создание тестового инцидента (для демо и мобильного приложения).
 */
public record CreateIncidentRequest(
        @NotNull Long ruleId,
        Long notifyEngineerId,
        String channel
) {
}

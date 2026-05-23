package com.monitoring.core.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Уведомление инженера об инциденте.
 */
public record Notification(
        Long id,
        Long incidentId,
        Long engineerId,
        String channel,
        Boolean delivered,
        LocalDateTime readAt
) {
    public Notification {
        Objects.requireNonNull(incidentId, "incidentId обязателен");
        Objects.requireNonNull(engineerId, "engineerId обязателен");
        Objects.requireNonNull(channel, "channel обязателен");
        Objects.requireNonNull(delivered, "delivered обязателен");
    }

    /**
     * Помечает уведомление прочитанным (ставит readAt на текущий момент).
     */
    public Notification markAsRead() {
        return new Notification(id, incidentId, engineerId, channel, delivered, LocalDateTime.now());
    }
}

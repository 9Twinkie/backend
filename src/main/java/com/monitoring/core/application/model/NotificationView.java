package com.monitoring.core.application.model;

import java.time.LocalDateTime;

public record NotificationView(
        Long id,
        Long incidentId,
        Long engineerId,
        String channel,
        Boolean delivered,
        LocalDateTime readAt,
        boolean read
) {
}

package com.monitoring.core.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Инцидент, созданный по сработавшему правилу. Статус меняется только доменными методами.
 */
public record Incident(
        Long id,
        Long ruleId,
        LocalDateTime timestamp,
        Status status,
        Long assignedEngineerId,
        LocalDateTime resolvedAt
) {
    public Incident {
        Objects.requireNonNull(timestamp, "timestamp обязателен");
        Objects.requireNonNull(status, "status обязателен");
    }

    /**
     * Подтверждает инцидент: только из NEW в CONFIRMED, назначает инженера.
     */
    public Incident confirm(Long engineerId) {
        Objects.requireNonNull(engineerId, "engineerId обязателен");
        if (status != Status.NEW) {
            throw new IllegalStateException("Подтвердить можно только инцидент в статусе NEW");
        }
        return new Incident(id, ruleId, timestamp, Status.CONFIRMED, engineerId, resolvedAt);
    }

    /**
     * Автоматическое закрытие при восстановлении метрики (планировщик алертов).
     */
    public Incident autoResolve() {
        if (status == Status.CLOSED) {
            throw new IllegalStateException("Инцидент уже закрыт");
        }
        return new Incident(id, ruleId, timestamp, Status.CLOSED, assignedEngineerId, LocalDateTime.now());
    }

    /**
     * Закрывает инцидент: только CONFIRMED тем же инженером, выставляет CLOSED и время разрешения.
     */
    public Incident close(Long engineerId) {
        Objects.requireNonNull(engineerId, "engineerId обязателен");
        if (status != Status.CONFIRMED) {
            throw new IllegalStateException("Закрыть можно только подтверждённый инцидент");
        }
        if (!Objects.equals(assignedEngineerId, engineerId)) {
            throw new IllegalStateException("Закрыть может только назначенный инженер");
        }
        return new Incident(id, ruleId, timestamp, Status.CLOSED, assignedEngineerId, LocalDateTime.now());
    }
}

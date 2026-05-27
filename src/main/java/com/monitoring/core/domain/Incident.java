package com.monitoring.core.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Инцидент: из правила в БД (legacy) или из firing-алерта Prometheus.
 */
public record Incident(
        Long id,
        Long ruleId,
        String prometheusFingerprint,
        String prometheusAlertName,
        String prometheusExpr,
        String prometheusSummary,
        String prometheusDescription,
        Severity prometheusSeverity,
        LocalDateTime timestamp,
        Status status,
        Long assignedEngineerId,
        LocalDateTime resolvedAt
) {
    public Incident {
        Objects.requireNonNull(timestamp, "timestamp обязателен");
        Objects.requireNonNull(status, "status обязателен");
    }

    public static Incident newFromRule(Long ruleId) {
        return new Incident(
                null, ruleId, null, null, null, null, null, null,
                LocalDateTime.now(), Status.NEW, null, null
        );
    }

    public static Incident newFromPrometheus(
            String fingerprint,
            String alertName,
            String expr,
            String summary,
            String description,
            Severity severity
    ) {
        Objects.requireNonNull(fingerprint, "fingerprint обязателен");
        return new Incident(
                null, null, fingerprint, alertName, expr, summary, description, severity,
                LocalDateTime.now(), Status.NEW, null, null
        );
    }

    public boolean isPrometheusSourced() {
        return prometheusFingerprint != null && !prometheusFingerprint.isBlank();
    }

    public Incident confirm(Long engineerId) {
        Objects.requireNonNull(engineerId, "engineerId обязателен");
        if (status != Status.NEW) {
            throw new IllegalStateException("Подтвердить можно только инцидент в статусе NEW");
        }
        return copyWith(status, engineerId, resolvedAt);
    }

    public Incident autoResolve() {
        if (status == Status.CLOSED) {
            throw new IllegalStateException("Инцидент уже закрыт");
        }
        return copyWith(Status.CLOSED, assignedEngineerId, LocalDateTime.now());
    }

    public Incident close(Long engineerId) {
        Objects.requireNonNull(engineerId, "engineerId обязателен");
        if (status != Status.CONFIRMED) {
            throw new IllegalStateException("Закрыть можно только подтверждённый инцидент");
        }
        if (!Objects.equals(assignedEngineerId, engineerId)) {
            throw new IllegalStateException("Закрыть может только назначенный инженер");
        }
        return copyWith(Status.CLOSED, assignedEngineerId, LocalDateTime.now());
    }

    private Incident copyWith(Status newStatus, Long engineerId, LocalDateTime resolved) {
        return new Incident(
                id, ruleId, prometheusFingerprint, prometheusAlertName, prometheusExpr,
                prometheusSummary, prometheusDescription, prometheusSeverity,
                timestamp, newStatus, engineerId, resolved
        );
    }
}

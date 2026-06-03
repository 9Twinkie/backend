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
        LocalDateTime resolvedAt,
        String trackerIssueKey,
        /**
         * Prometheus: true — алерт ещё firing; false — алерт погас, инцидент остаётся в работе.
         * null — не Prometheus-инцидент.
         */
        Boolean prometheusAlertActive,
        /** Комментарий инженера при закрытии. */
        String closeComment,
        /** id инженера, закрывшего инцидент. */
        Long closedByEngineerId
) {
    public Incident {
        Objects.requireNonNull(timestamp, "timestamp обязателен");
        Objects.requireNonNull(status, "status обязателен");
    }

    public static Incident newFromRule(Long ruleId) {
        return new Incident(
                null, ruleId, null, null, null, null, null, null,
                LocalDateTime.now(), Status.NEW, null, null, null, null, null, null
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
                LocalDateTime.now(), Status.NEW, null, null, null, true, null, null
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
        return copyWith(Status.CONFIRMED, engineerId, resolvedAt, prometheusAlertActive, closeComment, closedByEngineerId);
    }

    public Incident autoResolve() {
        if (status == Status.CLOSED) {
            throw new IllegalStateException("Инцидент уже закрыт");
        }
        return copyWith(Status.CLOSED, assignedEngineerId, LocalDateTime.now(), prometheusAlertActive, closeComment, closedByEngineerId);
    }

    public Incident close(Long engineerId) {
        Objects.requireNonNull(engineerId, "engineerId обязателен");
        if (status != Status.CONFIRMED) {
            throw new IllegalStateException("Закрыть можно только подтверждённый инцидент");
        }
        if (!Objects.equals(assignedEngineerId, engineerId)) {
            throw new IllegalStateException("Закрыть может только назначенный инженер");
        }
        return copyWith(Status.CLOSED, assignedEngineerId, LocalDateTime.now(), prometheusAlertActive, closeComment, closedByEngineerId);
    }

    private Incident copyWith(
            Status newStatus,
            Long engineerId,
            LocalDateTime resolved,
            Boolean alertActive,
            String closeComment,
            Long closedByEngineerId
    ) {
        return new Incident(
                id, ruleId, prometheusFingerprint, prometheusAlertName, prometheusExpr,
                prometheusSummary, prometheusDescription, prometheusSeverity,
                timestamp, newStatus, engineerId, resolved, trackerIssueKey, alertActive,
                closeComment, closedByEngineerId
        );
    }
}

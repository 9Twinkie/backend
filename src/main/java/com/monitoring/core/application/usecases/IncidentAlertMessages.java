package com.monitoring.core.application.usecases;

import com.monitoring.core.domain.AlertRule;
import com.monitoring.core.domain.Severity;

/**
 * Тексты для push / WebSocket (читаемые на мобильном клиенте).
 */
public final class IncidentAlertMessages {

    private IncidentAlertMessages() {
    }

    public static String created(AlertRule rule) {
        return severityLabel(rule.severity()) + ": " + rule.metricName();
    }

    public static String resolved(AlertRule rule) {
        return "Восстановлено: " + rule.metricName();
    }

    public static String severityLabel(Severity severity) {
        return switch (severity) {
            case CRITICAL -> "Критический";
            case HIGH -> "Высокий";
            case MEDIUM -> "Средний";
            case LOW -> "Низкий";
        };
    }

    /** Тип события для клиента: critical / alert / incident. */
    public static String eventTypeForSeverity(Severity severity) {
        return switch (severity) {
            case CRITICAL -> "critical";
            case HIGH -> "alert";
            default -> "incident";
        };
    }
}

package com.monitoring.core.application.model;

import com.monitoring.core.domain.Severity;

import java.util.Map;

/**
 * Активный (firing) алерт из Prometheus {@code GET /api/v1/rules?type=alert}.
 */
public record PrometheusFiringAlert(
        String fingerprint,
        String alertName,
        String expr,
        Severity severity,
        String summary,
        String description,
        Map<String, String> labels
) {
    /** Заголовок для push / списка: имя алерта. */
    public String displayTitle() {
        if (alertName != null && !alertName.isBlank()) {
            return alertName;
        }
        return expr;
    }

    /** Подзаголовок: description или summary. */
    public String displaySubtitle() {
        if (description != null && !description.isBlank()) {
            return description;
        }
        return summary;
    }

    public String displayMetric() {
        return displayTitle();
    }
}

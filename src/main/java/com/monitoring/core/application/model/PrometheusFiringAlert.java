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
        Map<String, String> labels
) {
    public String displayMetric() {
        if (expr != null && !expr.isBlank()) {
            return expr;
        }
        return alertName;
    }
}

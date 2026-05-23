package com.monitoring.core.application.model;

import com.monitoring.core.domain.Severity;
import com.monitoring.core.domain.Status;

import java.time.LocalDateTime;

/**
 * Инцидент с данными правила для отображения в API.
 */
public record IncidentView(
        Long id,
        Long ruleId,
        String metricName,
        String operator,
        Double threshold,
        Severity severity,
        Status status,
        LocalDateTime timestamp,
        Long assignedEngineerId,
        LocalDateTime resolvedAt
) {
}

package com.monitoring.core.application.model;

import com.monitoring.core.domain.Severity;
import com.monitoring.core.domain.Status;

import java.time.LocalDateTime;

/**
 * Инцидент с данными правила для отображения в API.
 *
 * @param assignedEngineerUsername логин инженера, взявшего инцидент в работу (null если NEW)
 * @param canAccept можно нажать «Принять» (только status=NEW)
 * @param canClose можно закрыть (CONFIRMED и назначен текущему пользователю)
 */
public record IncidentView(
        Long id,
        Long ruleId,
        /** Имя алерта Prometheus или метрика правила из БД — для заголовка в UI. */
        String metricName,
        /** Имя правила/алерта (для Prometheus = alertname). */
        String alertName,
        /** Человекочитаемый текст из annotations (description/summary). */
        String description,
        /** PromQL для графика (только Prometheus-инциденты). */
        String promql,
        String operator,
        Double threshold,
        Severity severity,
        Status status,
        LocalDateTime timestamp,
        Long assignedEngineerId,
        String assignedEngineerUsername,
        LocalDateTime resolvedAt,
        boolean canAccept,
        boolean canClose
) {
}

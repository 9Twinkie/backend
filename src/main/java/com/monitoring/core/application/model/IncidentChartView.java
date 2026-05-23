package com.monitoring.core.application.model;

import java.util.List;

/**
 * Готовые точки графика для карточки инцидента в мобильном приложении.
 */
public record IncidentChartView(
        Long incidentId,
        String metricName,
        String promql,
        Double threshold,
        int hours,
        List<ChartPoint> points
) {
}

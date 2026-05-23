package com.monitoring.core.application.ports.out.metrics;

import com.monitoring.core.application.model.ChartPoint;

import java.util.List;

/**
 * История значений метрики за интервал (Prometheus query_range).
 */
public interface MetricHistoryReader {

    List<ChartPoint> readRange(String promql, int hoursBack, String step);
}

package com.monitoring.adapters.outbound.prometheus;

import com.monitoring.core.application.model.ChartPoint;
import com.monitoring.core.application.ports.out.metrics.MetricHistoryReader;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class PrometheusMetricHistoryReader implements MetricHistoryReader {

    private final PrometheusHttpClient prometheus;

    public PrometheusMetricHistoryReader(PrometheusHttpClient prometheus) {
        this.prometheus = prometheus;
    }

    @Override
    public List<ChartPoint> readRange(String promql, int hoursBack, String step) {
        long end = Instant.now().getEpochSecond();
        long start = end - hoursBack * 3600L;
        return prometheus.queryRange(promql, start, end, step)
                .map(PrometheusChartParser::parseRangeResponse)
                .orElse(List.of());
    }
}

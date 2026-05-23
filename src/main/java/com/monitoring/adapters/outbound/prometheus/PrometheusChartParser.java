package com.monitoring.adapters.outbound.prometheus;

import com.fasterxml.jackson.databind.JsonNode;
import com.monitoring.core.application.model.ChartPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Разбор ответа Prometheus {@code query_range} в плоский список точек.
 */
public final class PrometheusChartParser {

    private PrometheusChartParser() {
    }

    public static List<ChartPoint> parseRangeResponse(JsonNode root) {
        var points = new ArrayList<ChartPoint>();
        if (root == null || !isSuccess(root)) {
            return points;
        }
        var result = root.path("data").path("result");
        if (!result.isArray() || result.isEmpty()) {
            return points;
        }
        var values = result.get(0).path("values");
        if (!values.isArray()) {
            return points;
        }
        for (var sample : values) {
            if (!sample.isArray() || sample.size() < 2) {
                continue;
            }
            long ts = sample.get(0).asLong();
            double value = Double.parseDouble(sample.get(1).asText("0"));
            points.add(new ChartPoint(ts, value));
        }
        return points;
    }

    private static boolean isSuccess(JsonNode root) {
        return root.hasNonNull("status") && "success".equals(root.get("status").asText());
    }
}

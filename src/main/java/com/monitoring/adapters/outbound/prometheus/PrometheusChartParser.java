package com.monitoring.adapters.outbound.prometheus;

import com.fasterxml.jackson.databind.JsonNode;
import com.monitoring.core.application.model.ChartPoint;
import com.monitoring.core.application.model.MetricSeriesView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Разбор ответа Prometheus {@code query_range} в точки графика.
 */
public final class PrometheusChartParser {

    private PrometheusChartParser() {
    }

    /** Первая серия (для инцидентов). */
    public static List<ChartPoint> parseRangeResponse(JsonNode root) {
        var series = parseAllSeries(root);
        return series.isEmpty() ? List.of() : series.getFirst().points();
    }

    public static List<MetricSeriesView> parseAllSeries(JsonNode root) {
        var series = new ArrayList<MetricSeriesView>();
        if (root == null || !PrometheusJsonArrays.isSuccess(root)) {
            return series;
        }
        var result = root.path("data").path("result");
        if (!result.isArray()) {
            return series;
        }
        for (var item : result) {
            var labels = readLabels(item.path("metric"));
            var points = readValues(item.path("values"));
            if (points.isEmpty()) {
                continue;
            }
            series.add(new MetricSeriesView(labels, formatLegend(labels), points));
        }
        return series;
    }

    private static Map<String, String> readLabels(JsonNode metricNode) {
        var labels = new HashMap<String, String>();
        if (metricNode == null || !metricNode.isObject()) {
            return labels;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = metricNode.fields();
        while (fields.hasNext()) {
            var entry = fields.next();
            labels.put(entry.getKey(), entry.getValue().asText());
        }
        return Map.copyOf(labels);
    }

    private static List<ChartPoint> readValues(JsonNode valuesNode) {
        var points = new ArrayList<ChartPoint>();
        if (!valuesNode.isArray()) {
            return points;
        }
        for (var sample : valuesNode) {
            if (!sample.isArray() || sample.size() < 2) {
                continue;
            }
            long ts = sample.get(0).asLong();
            double value = Double.parseDouble(sample.get(1).asText("0"));
            points.add(new ChartPoint(ts, value));
        }
        return points;
    }

    static String formatLegend(Map<String, String> labels) {
        if (labels == null || labels.isEmpty()) {
            return "series";
        }
        var name = labels.get("__name__");
        var parts = new ArrayList<String>();
        labels.entrySet().stream()
                .filter(e -> !"__name__".equals(e.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> parts.add(e.getKey() + "=\"" + e.getValue() + "\""));
        if (name == null) {
            return String.join(", ", parts);
        }
        if (parts.isEmpty()) {
            return name;
        }
        return name + "{" + String.join(", ", parts) + "}";
    }
}

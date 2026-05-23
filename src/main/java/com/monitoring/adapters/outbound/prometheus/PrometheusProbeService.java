package com.monitoring.adapters.outbound.prometheus;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Сводка «что видит Prometheus» для диагностики с бэкенда.
 */
@Service
public class PrometheusProbeService {

    private static final int METRIC_NAME_SAMPLE_LIMIT = 40;

    private final PrometheusHttpClient client;

    public PrometheusProbeService(PrometheusHttpClient client) {
        this.client = client;
    }

    public PrometheusOverview overview() {
        var upResponse = client.queryInstant("up");
        var targetsResponse = client.targets();
        var metricsResponse = client.metricNames();

        boolean reachable = upResponse.isPresent() && isSuccess(upResponse.get());
        var upSeries = upResponse.filter(this::isSuccess)
                .map(this::parseUpSeries)
                .orElse(List.of());
        var targets = targetsResponse.filter(this::isSuccess)
                .map(this::parseTargets)
                .orElse(TargetsSummary.empty());
        var metrics = metricsResponse.filter(this::isSuccess)
                .map(this::parseMetricNames)
                .orElse(MetricNamesSummary.empty());

        String error = null;
        if (!reachable) {
            error = "Не удалось выполнить query=up по адресу " + client.configuredBaseUrl()
                    + " — проверьте, что Prometheus запущен и URL верный";
        }

        return new PrometheusOverview(
                client.configuredBaseUrl(),
                reachable,
                error,
                upSeries,
                targets,
                metrics
        );
    }

    public Optional<JsonNode> runQuery(String promql) {
        return client.queryInstant(promql).filter(this::isSuccess);
    }

    private boolean isSuccess(JsonNode root) {
        return root.hasNonNull("status") && "success".equals(root.get("status").asText());
    }

    private List<UpSeriesRow> parseUpSeries(JsonNode root) {
        var result = new ArrayList<UpSeriesRow>();
        var data = root.path("data").path("result");
        if (!data.isArray()) {
            return result;
        }
        for (var node : data) {
            var metric = node.path("metric");
            var labels = new ArrayList<String>();
            metric.fields().forEachRemaining(e -> labels.add(e.getKey() + "=" + e.getValue().asText()));
            var value = node.path("value");
            var sampleValue = value.isArray() && value.size() > 1 ? value.get(1).asText() : "?";
            result.add(new UpSeriesRow(String.join(", ", labels), sampleValue));
        }
        return result;
    }

    private TargetsSummary parseTargets(JsonNode root) {
        int up = 0;
        int down = 0;
        int unknown = 0;
        var samples = new ArrayList<TargetRow>();

        var active = root.path("data").path("activeTargets");
        if (!active.isArray()) {
            return TargetsSummary.empty();
        }
        for (var t : active) {
            var health = t.path("health").asText("unknown");
            switch (health) {
                case "up" -> up++;
                case "down" -> down++;
                default -> unknown++;
            }
            if (samples.size() < 25) {
                samples.add(new TargetRow(
                        t.path("scrapePool").asText(""),
                        t.path("discoveredLabels").path("job").asText(
                                t.path("labels").path("job").asText("")
                        ),
                        t.path("scrapeUrl").asText(""),
                        health,
                        t.path("lastError").asText("")
                ));
            }
        }
        return new TargetsSummary(active.size(), up, down, unknown, samples);
    }

    private MetricNamesSummary parseMetricNames(JsonNode root) {
        var names = new ArrayList<String>();
        var data = root.path("data");
        if (data.isArray()) {
            data.forEach(n -> names.add(n.asText()));
        }
        names.sort(Comparator.naturalOrder());
        var sample = names.stream().limit(METRIC_NAME_SAMPLE_LIMIT).toList();
        return new MetricNamesSummary(names.size(), sample);
    }

    public record PrometheusOverview(
            String prometheusUrl,
            boolean reachable,
            String error,
            List<UpSeriesRow> upMetrics,
            TargetsSummary targets,
            MetricNamesSummary metrics
    ) {
    }

    public record UpSeriesRow(String labels, String value) {
    }

    public record TargetRow(String scrapePool, String job, String scrapeUrl, String health, String lastError) {
    }

    public record TargetsSummary(int total, int up, int down, int unknown, List<TargetRow> samples) {
        static TargetsSummary empty() {
            return new TargetsSummary(0, 0, 0, 0, List.of());
        }
    }

    public record MetricNamesSummary(int totalCount, List<String> sampleNames) {
        static MetricNamesSummary empty() {
            return new MetricNamesSummary(0, List.of());
        }
    }
}

package com.monitoring.adapters.inbound.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.monitoring.adapters.outbound.prometheus.MonitoringDashboardService;
import com.monitoring.adapters.outbound.prometheus.PrometheusMetricExplorerService;
import com.monitoring.config.security.SecurityCurrentUser;
import com.monitoring.core.application.model.MetricChartView;
import com.monitoring.core.application.model.MetricLabelValuesView;
import com.monitoring.core.application.model.MetricLabelsView;
import com.monitoring.core.application.model.MetricNamesView;
import com.monitoring.core.application.model.MetricSuggestView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Данные для вкладок «Мониторинг», поиск метрик и графики (мобильное приложение).
 */
@RestController
@RequestMapping("/monitoring")
public class MonitoringController {

    private final MonitoringDashboardService dashboard;
    private final PrometheusMetricExplorerService metrics;
    private final SecurityCurrentUser currentUser;

    public MonitoringController(
            MonitoringDashboardService dashboard,
            PrometheusMetricExplorerService metrics,
            SecurityCurrentUser currentUser
    ) {
        this.dashboard = dashboard;
        this.metrics = metrics;
        this.currentUser = currentUser;
    }

    @GetMapping("/summary")
    public MonitoringDashboardService.MonitoringSummary summary() {
        return dashboard.summary(currentUser.username(), currentUser.isAdmin());
    }

    @GetMapping("/objects")
    public List<MonitoringDashboardService.MonitoringObject> objects() {
        return dashboard.summary(currentUser.username(), currentUser.isAdmin()).objects();
    }

    @GetMapping("/metrics/presets")
    public List<MonitoringDashboardService.MetricPreset> presets() {
        return dashboard.metricPresets();
    }

    /**
     * Поиск имён метрик (как автодополнение в Prometheus).
     * {@code GET /monitoring/metrics/names?q=node&limit=50} или {@code ?match=node}
     */
    @GetMapping("/metrics/names")
    public ResponseEntity<MetricNamesView> metricNames(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String match,
            @RequestParam(defaultValue = "50") int limit
    ) {
        var search = firstNonBlank(q, match);
        return metrics.searchMetricNames(search, limit)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(503).build());
    }

    /**
     * Подсказки: имена метрик + готовые PromQL (instant / rate).
     */
    @GetMapping("/metrics/suggest")
    public ResponseEntity<MetricSuggestView> suggest(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "30") int limit
    ) {
        return metrics.suggest(q, limit)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(503).build());
    }

    /** Все label names в TSDB. */
    @GetMapping("/metrics/labels")
    public ResponseEntity<MetricLabelsView> labels(
            @RequestParam(required = false) String match
    ) {
        return metrics.listLabels(match)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(503).build());
    }

    /**
     * Значения label (например job, instance).
     * {@code match} — selector без внешних скобок, например {@code job="node"}.
     */
    @GetMapping("/metrics/labels/{label}/values")
    public ResponseEntity<MetricLabelValuesView> labelValues(
            @PathVariable String label,
            @RequestParam(required = false) String match,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return metrics.listLabelValues(label, match, limit)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(503).build());
    }

    /**
     * График: готовые точки по сериям (удобно для мобилки).
     */
    @GetMapping("/metrics/chart")
    public ResponseEntity<MetricChartView> chart(
            @RequestParam String query,
            @RequestParam(defaultValue = "6") int hours,
            @RequestParam(defaultValue = "60s") String step
    ) {
        return metrics.chart(query, hours, step)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(503).build());
    }

    /**
     * Сырой ответ Prometheus query_range (для отладки / продвинутого клиента).
     */
    @GetMapping("/metrics/range")
    public ResponseEntity<JsonNode> range(
            @RequestParam String query,
            @RequestParam(defaultValue = "6") int hours,
            @RequestParam(defaultValue = "60s") String step
    ) {
        return dashboard.queryRange(query, hours, step)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(503).build());
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }
}

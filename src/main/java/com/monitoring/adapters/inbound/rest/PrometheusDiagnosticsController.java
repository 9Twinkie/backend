package com.monitoring.adapters.inbound.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.monitoring.adapters.outbound.prometheus.MonitoringDashboardService;
import com.monitoring.adapters.outbound.prometheus.PrometheusProbeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Диагностика: что бэкенд реально читает из Prometheus на машине (таргеты, up, имена метрик).
 * Требуется JWT (любой авторизованный пользователь).
 */
@RestController
@RequestMapping("/monitoring/prometheus")
public class PrometheusDiagnosticsController {

    private final PrometheusProbeService probeService;
    private final MonitoringDashboardService dashboard;

    public PrometheusDiagnosticsController(
            PrometheusProbeService probeService,
            MonitoringDashboardService dashboard
    ) {
        this.probeService = probeService;
        this.dashboard = dashboard;
    }

    /**
     * Сводка: доступность API, серии {@code up}, scrape-таргеты, образец имён метрик.
     */
    @GetMapping("/overview")
    public PrometheusProbeService.PrometheusOverview overview() {
        return probeService.overview();
    }

    /**
     * Произвольный instant-запрос PromQL — сырой JSON ответа Prometheus (поле {@code data}).
     */
    @GetMapping("/query")
    public ResponseEntity<JsonNode> query(@RequestParam String promql) {
        return probeService.runQuery(promql)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(503).build());
    }

    /**
     * История метрики за период (прокси {@code GET /api/v1/query_range} Prometheus).
     * URL для мобильного клиента: {@code /monitoring/prometheus/range?query=...&hours=1&step=15s}
     */
    @GetMapping({"/range", "/query_range"})
    public ResponseEntity<JsonNode> range(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int hours,
            @RequestParam(defaultValue = "15s") String step
    ) {
        return dashboard.queryRange(query, hours, step)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(503).build());
    }
}

package com.monitoring.adapters.inbound.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.monitoring.adapters.outbound.prometheus.MonitoringDashboardService;
import com.monitoring.config.security.SecurityCurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Данные для вкладок «Мониторинг» и «Графики» мобильного приложения.
 */
@RestController
@RequestMapping("/monitoring")
public class MonitoringController {

    private final MonitoringDashboardService dashboard;
    private final SecurityCurrentUser currentUser;

    public MonitoringController(MonitoringDashboardService dashboard, SecurityCurrentUser currentUser) {
        this.dashboard = dashboard;
        this.currentUser = currentUser;
    }

  /** Сводка: критические инциденты, серверы online/total, объекты. */
    @GetMapping("/summary")
    public MonitoringDashboardService.MonitoringSummary summary() {
        return dashboard.summary(currentUser.username(), currentUser.isAdmin());
    }

  /** Объекты мониторинга (таргеты Prometheus + открытые инциденты). */
    @GetMapping("/objects")
    public List<MonitoringDashboardService.MonitoringObject> objects() {
        return dashboard.summary(currentUser.username(), currentUser.isAdmin()).objects();
    }

    @GetMapping("/metrics/presets")
    public List<MonitoringDashboardService.MetricPreset> presets() {
        return dashboard.metricPresets();
    }

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
}

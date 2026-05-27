package com.monitoring.adapters.outbound.prometheus;

import com.fasterxml.jackson.databind.JsonNode;
import com.monitoring.core.application.model.IncidentView;
import com.monitoring.core.application.usecases.IncidentApplicationService;
import com.monitoring.core.domain.Severity;
import com.monitoring.core.domain.Status;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Сводка для экрана «Мониторинг» в мобильном приложении.
 */
@Service
public class MonitoringDashboardService {

    private final PrometheusHttpClient prometheus;
    private final IncidentApplicationService incidents;

    public MonitoringDashboardService(PrometheusHttpClient prometheus, IncidentApplicationService incidents) {
        this.prometheus = prometheus;
        this.incidents = incidents;
    }

    public MonitoringSummary summary(String username, boolean admin) {
        var incidentViews = incidents.listForUser(username, admin);
        long critical = incidentViews.stream()
                .filter(i -> i.status() != Status.CLOSED)
                .filter(i -> i.severity() == Severity.HIGH || i.severity() == Severity.CRITICAL)
                .count();

        int online = 0;
        int total = 0;
        var objects = new ArrayList<MonitoringObject>();

        var targetsJson = prometheus.targets();
        if (targetsJson.isPresent() && isSuccess(targetsJson.get())) {
            var active = targetsJson.get().path("data").path("activeTargets");
            if (active.isArray()) {
                for (var t : active) {
                    total++;
                    var health = t.path("health").asText("unknown");
                    if ("up".equals(health)) {
                        online++;
                    }
                    var job = t.path("labels").path("job").asText(
                            t.path("discoveredLabels").path("job").asText("unknown")
                    );
                    var instance = t.path("labels").path("instance").asText(
                            t.path("discoveredLabels").path("instance").asText("")
                    );
                    objects.add(new MonitoringObject(
                            "target:" + job + ":" + instance,
                            job + " (" + instance + ")",
                            "server",
                            "up".equals(health) ? "online" : "offline",
                            job,
                            instance,
                            health
                    ));
                }
            }
        }

        for (var inc : incidentViews) {
            if (inc.status() == Status.CLOSED) {
                continue;
            }
            var label = inc.description() != null && !inc.description().isBlank()
                    ? inc.metricName() + " — " + inc.description()
                    : inc.metricName() + " — " + inc.severity();
            objects.add(new MonitoringObject(
                    "incident:" + inc.id(),
                    label,
                    "incident",
                    inc.status().name(),
                    inc.metricName(),
                    null,
                    inc.status().name()
            ));
        }

        return new MonitoringSummary(critical, online, total, objects);
    }

    public List<MetricPreset> metricPresets() {
        return List.of(
                new MetricPreset("cpu", "CPU (node)", """
                        100 - (avg(rate(node_cpu_seconds_total{mode="idle",job="node"}[5m])) * 100)"""),
                new MetricPreset("memory", "Память (node)", """
                        (1 - (node_memory_MemAvailable_bytes{job="node"} / node_memory_MemTotal_bytes{job="node"})) * 100"""),
                new MetricPreset("up", "Доступность (up)", "up")
        );
    }

    public Optional<JsonNode> queryRange(String promql, int hoursBack, String step) {
        long end = Instant.now().getEpochSecond();
        long start = end - hoursBack * 3600L;
        return prometheus.queryRange(promql, start, end, step);
    }

    private static boolean isSuccess(JsonNode root) {
        return root.hasNonNull("status") && "success".equals(root.get("status").asText());
    }

    public record MonitoringSummary(
            long criticalIncidents,
            int serversOnline,
            int serversTotal,
            List<MonitoringObject> objects
    ) {
    }

    public record MonitoringObject(
            String id,
            String name,
            String type,
            String status,
            String job,
            String instance,
            String detail
    ) {
    }

    public record MetricPreset(String id, String title, String promql) {
    }
}

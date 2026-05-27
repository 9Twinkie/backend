package com.monitoring.core.application.usecases;

import com.monitoring.core.application.model.IncidentChartView;
import com.monitoring.core.application.ports.out.metrics.MetricHistoryReader;
import com.monitoring.core.application.ports.out.repositories.AlertRuleRepository;
import com.monitoring.core.application.ports.out.repositories.EngineerRepository;
import com.monitoring.core.application.ports.out.repositories.IncidentRepository;
import com.monitoring.core.domain.Incident;
import com.monitoring.core.domain.Status;

import java.util.Optional;

/**
 * История метрики инцидента через Prometheus query_range (прокси бэкенда).
 */
public class IncidentChartService {

    private final IncidentRepository incidents;
    private final AlertRuleRepository alertRules;
    private final EngineerRepository engineers;
    private final MetricHistoryReader metricHistory;

    public IncidentChartService(
            IncidentRepository incidents,
            AlertRuleRepository alertRules,
            EngineerRepository engineers,
            MetricHistoryReader metricHistory
    ) {
        this.incidents = incidents;
        this.alertRules = alertRules;
        this.engineers = engineers;
        this.metricHistory = metricHistory;
    }

    public Optional<IncidentChartView> chartForIncident(
            Long incidentId,
            String username,
            boolean admin,
            int hours,
            String step
    ) {
        return incidents.findById(incidentId)
                .filter(inc -> canEngineerSee(inc, admin))
                .flatMap(inc -> buildChart(inc, hours, step));
    }

    private Optional<IncidentChartView> buildChart(Incident incident, int hours, String step) {
        String promql;
        String metricName;
        double threshold;

        if (incident.isPrometheusSourced()) {
            promql = incident.prometheusExpr();
            if (promql == null || promql.isBlank()) {
                return Optional.empty();
            }
            metricName = promql;
            threshold = 0.0;
        } else {
            if (incident.ruleId() == null) {
                return Optional.empty();
            }
            var rule = alertRules.findById(incident.ruleId());
            if (rule.isEmpty()) {
                return Optional.empty();
            }
            var alertRule = rule.get();
            promql = PromqlResolver.toPromql(alertRule);
            metricName = alertRule.metricName();
            threshold = alertRule.threshold();
        }

        var points = metricHistory.readRange(promql, hours, step);
        return Optional.of(new IncidentChartView(
                incident.id(),
                metricName,
                promql,
                threshold,
                hours,
                points
        ));
    }

    private boolean canEngineerSee(Incident inc, boolean admin) {
        return admin || inc.status() == Status.NEW || inc.status() == Status.CONFIRMED;
    }
}

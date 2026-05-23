package com.monitoring.core.application.usecases;

import com.monitoring.core.application.model.IncidentChartView;
import com.monitoring.core.application.ports.out.metrics.MetricHistoryReader;
import com.monitoring.core.application.ports.out.repositories.AlertRuleRepository;
import com.monitoring.core.application.ports.out.repositories.EngineerRepository;
import com.monitoring.core.application.ports.out.repositories.IncidentRepository;
import com.monitoring.core.domain.Incident;
import com.monitoring.core.domain.Status;

import java.util.NoSuchElementException;
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
                .filter(inc -> admin || canEngineerSee(inc, username))
                .flatMap(inc -> buildChart(inc, hours, step));
    }

    private Optional<IncidentChartView> buildChart(Incident incident, int hours, String step) {
        var rule = alertRules.findById(incident.ruleId());
        if (rule.isEmpty()) {
            return Optional.empty();
        }
        var alertRule = rule.get();
        var promql = PromqlResolver.toPromql(alertRule);
        var points = metricHistory.readRange(promql, hours, step);
        return Optional.of(new IncidentChartView(
                incident.id(),
                alertRule.metricName(),
                promql,
                alertRule.threshold(),
                hours,
                points
        ));
    }

    private boolean canEngineerSee(Incident inc, String username) {
        var engineerId = engineers.findByUsername(username)
                .map(e -> e.id())
                .orElseThrow(() -> new NoSuchElementException("Пользователь не найден: " + username));
        return inc.status() == Status.NEW
                || (inc.assignedEngineerId() != null && inc.assignedEngineerId().equals(engineerId));
    }
}

package com.monitoring.adapters.outbound.prometheus;

import com.monitoring.core.application.model.PrometheusFiringAlert;
import com.monitoring.core.application.ports.out.metrics.PrometheusFiringAlertsReader;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PrometheusRulesFiringAlertsReader implements PrometheusFiringAlertsReader {

    private final PrometheusHttpClient prometheus;

    public PrometheusRulesFiringAlertsReader(PrometheusHttpClient prometheus) {
        this.prometheus = prometheus;
    }

    @Override
    public List<PrometheusFiringAlert> listFiringAlerts() {
        return prometheus.rules("alert")
                .map(PrometheusRulesAlertParser::parse)
                .orElse(List.of());
    }
}

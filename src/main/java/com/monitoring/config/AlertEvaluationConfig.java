package com.monitoring.config;

import com.monitoring.core.application.ports.out.IncidentEventNotifier;
import com.monitoring.core.application.ports.out.metrics.MetricSampleReader;
import com.monitoring.core.application.ports.out.metrics.PrometheusFiringAlertsReader;
import com.monitoring.core.application.ports.out.repositories.AlertRuleRepository;
import com.monitoring.core.application.ports.out.repositories.IncidentRepository;
import com.monitoring.core.application.ports.out.repositories.NotificationRepository;
import com.monitoring.core.application.usecases.AlertEvaluationService;
import com.monitoring.core.application.usecases.PrometheusAlertSyncService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AlertEvaluationProperties.class)
public class AlertEvaluationConfig {

    @Bean
    public AlertEvaluationService alertEvaluationService(
            AlertRuleRepository alertRules,
            IncidentRepository incidents,
            NotificationRepository notifications,
            MetricSampleReader metrics,
            IncidentEventNotifier eventNotifier,
            AlertEvaluationProperties properties
    ) {
        return new AlertEvaluationService(
                alertRules,
                incidents,
                notifications,
                metrics,
                eventNotifier,
                properties.getDefaultNotifyEngineerId()
        );
    }

    @Bean
    public PrometheusAlertSyncService prometheusAlertSyncService(
            PrometheusFiringAlertsReader firingAlerts,
            IncidentRepository incidents,
            NotificationRepository notifications,
            IncidentEventNotifier eventNotifier,
            AlertEvaluationProperties properties
    ) {
        return new PrometheusAlertSyncService(
                firingAlerts,
                incidents,
                notifications,
                eventNotifier,
                properties.getDefaultNotifyEngineerId()
        );
    }
}

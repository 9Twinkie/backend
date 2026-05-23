package com.monitoring.adapters.inbound.scheduler;

import com.monitoring.config.AlertEvaluationProperties;
import com.monitoring.core.application.usecases.AlertEvaluationService;
import com.monitoring.core.application.usecases.PrometheusAlertSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Периодическая синхронизация инцидентов: из Prometheus (rules) или из alert_rules в БД (legacy).
 */
@Component
@ConditionalOnProperty(prefix = "monitoring.alert-evaluation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AlertEvaluationScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlertEvaluationScheduler.class);

    private final AlertEvaluationService databaseEvaluation;
    private final PrometheusAlertSyncService prometheusSync;
    private final AlertEvaluationProperties properties;

    public AlertEvaluationScheduler(
            AlertEvaluationService databaseEvaluation,
            PrometheusAlertSyncService prometheusSync,
            AlertEvaluationProperties properties
    ) {
        this.databaseEvaluation = databaseEvaluation;
        this.prometheusSync = prometheusSync;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${monitoring.alert-evaluation.interval-ms:1000}")
    public void run() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            if (properties.isPrometheusSource()) {
                prometheusSync.syncFromPrometheus();
            } else {
                int created = databaseEvaluation.evaluateAllActiveRules();
                if (created > 0) {
                    log.info("Цикл оценки правил БД: создано новых инцидентов {}", created);
                }
            }
        } catch (Exception ex) {
            log.error("Ошибка цикла синхронизации алертов", ex);
        }
    }
}

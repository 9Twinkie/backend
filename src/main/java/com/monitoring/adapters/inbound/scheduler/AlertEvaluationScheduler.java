package com.monitoring.adapters.inbound.scheduler;

import com.monitoring.config.AlertEvaluationProperties;
import com.monitoring.core.application.usecases.AlertEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Периодически опрашивает Prometheus и создаёт инциденты по сработавшим правилам.
 */
@Component
@ConditionalOnProperty(prefix = "monitoring.alert-evaluation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AlertEvaluationScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlertEvaluationScheduler.class);

    private final AlertEvaluationService evaluationService;
    private final AlertEvaluationProperties properties;

    public AlertEvaluationScheduler(AlertEvaluationService evaluationService, AlertEvaluationProperties properties) {
        this.evaluationService = evaluationService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${monitoring.alert-evaluation.interval-ms:1000}")
    public void run() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            int created = evaluationService.evaluateAllActiveRules();
            if (created > 0) {
                log.info("Цикл оценки правил: создано новых инцидентов {}", created);
            }
        } catch (Exception ex) {
            log.error("Ошибка цикла оценки правил алертов", ex);
        }
    }
}

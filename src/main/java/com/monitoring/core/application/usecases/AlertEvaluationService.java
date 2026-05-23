package com.monitoring.core.application.usecases;

import com.monitoring.core.application.ports.out.IncidentEventNotifier;
import com.monitoring.core.application.ports.out.metrics.MetricSampleReader;
import com.monitoring.core.application.ports.out.repositories.AlertRuleRepository;
import com.monitoring.core.application.ports.out.repositories.IncidentRepository;
import com.monitoring.core.application.ports.out.repositories.NotificationRepository;
import com.monitoring.core.domain.AlertRule;
import com.monitoring.core.domain.Incident;
import com.monitoring.core.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Оценка правил по метрикам Prometheus: создание инцидентов при срабатывании и автозакрытие при восстановлении.
 */
public class AlertEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(AlertEvaluationService.class);

    private final AlertRuleRepository alertRules;
    private final IncidentRepository incidents;
    private final NotificationRepository notifications;
    private final MetricSampleReader metrics;
    private final IncidentEventNotifier eventNotifier;
    private final long defaultNotifyEngineerId;

    public AlertEvaluationService(
            AlertRuleRepository alertRules,
            IncidentRepository incidents,
            NotificationRepository notifications,
            MetricSampleReader metrics,
            IncidentEventNotifier eventNotifier,
            long defaultNotifyEngineerId
    ) {
        this.alertRules = alertRules;
        this.incidents = incidents;
        this.notifications = notifications;
        this.metrics = metrics;
        this.eventNotifier = eventNotifier;
        this.defaultNotifyEngineerId = defaultNotifyEngineerId;
    }

    /**
     * @return число созданных инцидентов за цикл
     */
    public int evaluateAllActiveRules() {
        int created = 0;
        for (var rule : alertRules.findActiveRules()) {
            if (tryCreateIncident(rule)) {
                created++;
            }
        }
        int resolved = resolveRecoveredIncidents();
        if (resolved > 0) {
            log.info("Цикл оценки правил: автозакрыто инцидентов {}", resolved);
        }
        return created;
    }

    private boolean tryCreateIncident(AlertRule rule) {
        if (!isRuleFiring(rule)) {
            return false;
        }
        if (incidents.findOpenByRuleId(rule.id()).isPresent()) {
            log.debug("Правило id={} сработало, но открытый инцидент уже есть", rule.id());
            return false;
        }
        var incident = incidents.create(Incident.newFromRule(rule.id()));
        var notification = notifications.create(new Notification(
                null,
                incident.id(),
                defaultNotifyEngineerId,
                "push",
                false,
                null
        ));
        var message = IncidentAlertMessages.created(rule);
        eventNotifier.incidentCreated(
                incident.id(),
                notification.id(),
                rule.severity(),
                rule.metricName(),
                message
        );
        log.info("Авто-инцидент id={} по правилу id={} ({})", incident.id(), rule.id(), rule.metricName());
        return true;
    }

    /**
     * Закрывает открытые инциденты, если метрика по их правилу больше не нарушает порог (в т.ч. для отключённых правил).
     */
    private int resolveRecoveredIncidents() {
        int resolved = 0;
        for (var open : incidents.findAllOpen()) {
            if (open.isPrometheusSourced() || open.ruleId() == null) {
                continue;
            }
            var rule = alertRules.findById(open.ruleId());
            if (rule.isEmpty()) {
                log.warn("Открытый инцидент id={} ссылается на несуществующее правило {}", open.id(), open.ruleId());
                continue;
            }
            if (isRuleFiring(rule.get())) {
                continue;
            }
            var closed = open.autoResolve();
            incidents.updateStatus(closed.id(), closed.status().name(), closed.assignedEngineerId());
            var alertRule = rule.get();
            eventNotifier.incidentResolved(
                    closed.id(),
                    alertRule.metricName(),
                    IncidentAlertMessages.resolved(alertRule)
            );
            log.info(
                    "Автозакрытие инцидента id={} — метрика восстановилась (правило id={}, {})",
                    closed.id(),
                    alertRule.id(),
                    alertRule.metricName()
            );
            resolved++;
        }
        return resolved;
    }

    private boolean isRuleFiring(AlertRule rule) {
        var promql = PromqlResolver.toPromql(rule);
        Optional<Double> value = metrics.readInstant(promql);
        if (value.isEmpty()) {
            log.debug("Нет данных Prometheus для правила id={} promql={}", rule.id(), promql);
            return false;
        }
        double actual = value.get();
        boolean firing = rule.evaluate(actual);
        if (!firing) {
            log.debug("Правило id={} не сработало: actual={} {} {}", rule.id(), actual, rule.operator(), rule.threshold());
        }
        return firing;
    }
}

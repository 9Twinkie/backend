package com.monitoring.core.application.usecases;

import com.monitoring.core.application.model.IncidentView;
import com.monitoring.core.application.ports.in.CloseIncidentUseCase;
import com.monitoring.core.application.ports.in.ConfirmIncidentUseCase;
import com.monitoring.core.application.ports.in.GetIncidentUseCase;
import com.monitoring.core.application.ports.in.ListIncidentsUseCase;
import com.monitoring.core.application.ports.out.IncidentEventNotifier;
import com.monitoring.core.application.ports.out.repositories.AlertRuleRepository;
import com.monitoring.core.application.ports.out.repositories.EngineerRepository;
import com.monitoring.core.application.ports.out.repositories.IncidentRepository;
import com.monitoring.core.application.ports.out.repositories.NotificationRepository;
import com.monitoring.core.domain.Incident;
import com.monitoring.core.domain.Notification;
import com.monitoring.core.domain.Severity;
import com.monitoring.core.domain.Status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Сценарии работы с инцидентами.
 */
public class IncidentApplicationService implements
        ListIncidentsUseCase,
        GetIncidentUseCase,
        ConfirmIncidentUseCase,
        CloseIncidentUseCase {

    private final IncidentRepository incidents;
    private final AlertRuleRepository alertRules;
    private final EngineerRepository engineers;
    private final NotificationRepository notifications;
    private final IncidentEventNotifier eventNotifier;

    public IncidentApplicationService(
            IncidentRepository incidents,
            AlertRuleRepository alertRules,
            EngineerRepository engineers,
            NotificationRepository notifications,
            IncidentEventNotifier eventNotifier
    ) {
        this.incidents = incidents;
        this.alertRules = alertRules;
        this.engineers = engineers;
        this.notifications = notifications;
        this.eventNotifier = eventNotifier;
    }

    /**
     * Ручное создание инцидента (демо / тест мобильного приложения) + уведомление инженеру.
     */
    public IncidentView createManual(Long ruleId, Long notifyEngineerId, String channel) {
        var rule = alertRules.findById(ruleId)
                .orElseThrow(() -> new NoSuchElementException("Правило не найдено: " + ruleId));
        long engineerId = notifyEngineerId != null ? notifyEngineerId : 2L;
        engineers.findById(engineerId)
                .orElseThrow(() -> new NoSuchElementException("Инженер не найден: " + engineerId));
        var channelName = channel != null && !channel.isBlank() ? channel : "push";

        var created = incidents.create(Incident.newFromRule(ruleId));
        var notification = notifications.create(new Notification(
                null,
                created.id(),
                engineerId,
                channelName,
                false,
                null
        ));
        var message = IncidentAlertMessages.created(rule);
        eventNotifier.incidentCreated(
                created.id(),
                notification.id(),
                rule.severity(),
                rule.metricName(),
                message
        );
        return toView(created);
    }

    @Override
    public List<IncidentView> listForUser(String username, boolean admin) {
        List<Incident> list = admin
                ? incidents.findAll()
                : incidents.findVisibleToEngineer(requireEngineerId(username));
        return list.stream().map(this::toView).toList();
    }

    @Override
    public Optional<IncidentView> getById(Long id, String username, boolean admin) {
        return incidents.findById(id)
                .filter(inc -> admin || canEngineerSee(inc, username))
                .map(this::toView);
    }

    @Override
    public IncidentView confirm(Long incidentId, String username) {
        var engineerId = requireEngineerId(username);
        var current = incidents.findById(incidentId)
                .orElseThrow(() -> new NoSuchElementException("Инцидент не найден: " + incidentId));
        var updated = current.confirm(engineerId);
        incidents.updateStatus(updated.id(), updated.status().name(), updated.assignedEngineerId());
        return toView(incidents.findById(incidentId).orElseThrow());
    }

    @Override
    public IncidentView close(Long incidentId, String username) {
        var engineerId = requireEngineerId(username);
        var current = incidents.findById(incidentId)
                .orElseThrow(() -> new NoSuchElementException("Инцидент не найден: " + incidentId));
        var updated = current.close(engineerId);
        incidents.updateStatus(updated.id(), updated.status().name(), updated.assignedEngineerId());
        return toView(incidents.findById(incidentId).orElseThrow());
    }

    private boolean canEngineerSee(Incident inc, String username) {
        var engineerId = requireEngineerId(username);
        return inc.status() == Status.NEW
                || (inc.assignedEngineerId() != null && inc.assignedEngineerId().equals(engineerId));
    }

    private Long requireEngineerId(String username) {
        return engineers.findByUsername(username)
                .map(e -> e.id())
                .orElseThrow(() -> new NoSuchElementException("Пользователь не найден: " + username));
    }

    private IncidentView toView(Incident incident) {
        if (incident.isPrometheusSourced()) {
            var metricName = incident.prometheusExpr() != null && !incident.prometheusExpr().isBlank()
                    ? incident.prometheusExpr()
                    : incident.prometheusAlertName();
            var severity = incident.prometheusSeverity() != null ? incident.prometheusSeverity() : Severity.HIGH;
            return new IncidentView(
                    incident.id(),
                    incident.ruleId(),
                    metricName,
                    "prometheus",
                    0.0,
                    severity,
                    incident.status(),
                    incident.timestamp(),
                    incident.assignedEngineerId(),
                    incident.resolvedAt()
            );
        }
        var rule = incident.ruleId() != null ? alertRules.findById(incident.ruleId()).orElse(null) : null;
        var metricName = rule != null ? rule.metricName() : "unknown";
        var operator = rule != null ? rule.operator() : "?";
        var threshold = rule != null ? rule.threshold() : 0.0;
        var severity = rule != null ? rule.severity() : Severity.LOW;
        return new IncidentView(
                incident.id(),
                incident.ruleId(),
                metricName,
                operator,
                threshold,
                severity,
                incident.status(),
                incident.timestamp(),
                incident.assignedEngineerId(),
                incident.resolvedAt()
        );
    }
}

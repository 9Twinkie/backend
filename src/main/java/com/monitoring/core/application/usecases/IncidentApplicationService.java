package com.monitoring.core.application.usecases;

import com.monitoring.config.SiteProperties;
import com.monitoring.core.application.TrackerAssigneeResolver;
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

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Сценарии работы с инцидентами (очередь Service Desk).
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
    private final TrackerIncidentSyncService trackerSync;
    private final SiteProperties siteProperties;

    public IncidentApplicationService(
            IncidentRepository incidents,
            AlertRuleRepository alertRules,
            EngineerRepository engineers,
            NotificationRepository notifications,
            IncidentEventNotifier eventNotifier,
            TrackerIncidentSyncService trackerSync,
            SiteProperties siteProperties
    ) {
        this.incidents = incidents;
        this.alertRules = alertRules;
        this.engineers = engineers;
        this.notifications = notifications;
        this.eventNotifier = eventNotifier;
        this.trackerSync = trackerSync;
        this.siteProperties = siteProperties;
    }

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
        return toView(created, null, null);
    }

    @Override
    public List<IncidentView> listForUser(String username, boolean admin) {
        return listForUser(username, admin, "active");
    }

    /**
     * @param scope active — NEW + CONFIRMED (очередь); history — CLOSED; all — всё (для admin)
     */
    public List<IncidentView> listForUser(String username, boolean admin, String scope) {
        var engineerId = requireEngineerId(username);
        List<Incident> list = switch (scope == null ? "active" : scope.toLowerCase()) {
            case "history" -> incidents.findClosed();
            case "all" -> listAllForSync(admin);
            default -> admin ? incidents.findActiveForEngineers() : incidents.findActiveForEngineers();
        };
        return list.stream()
                .map(inc -> toView(inc, engineerId, username))
                .toList();
    }

    @Override
    public Optional<IncidentView> getById(Long id, String username, boolean admin) {
        var engineerId = requireEngineerId(username);
        return incidents.findById(id)
                .filter(inc -> canUserSee(inc, admin))
                .map(inc -> toView(inc, engineerId, username));
    }

    @Override
    public IncidentView confirm(Long incidentId, String username) {
        var engineerId = requireEngineerId(username);
        var current = incidents.findById(incidentId)
                .orElseThrow(() -> new NoSuchElementException("Инцидент не найден: " + incidentId));
        if (current.status() == Status.CONFIRMED) {
            var assignee = resolveAssigneeUsername(current.assignedEngineerId());
            throw new IllegalStateException(
                    "Инцидент уже в работе" + (assignee != null ? " у " + assignee : ""));
        }
        var updated = current.confirm(engineerId);
        incidents.updateStatus(updated.id(), updated.status().name(), updated.assignedEngineerId());
        trackerSync.onIncidentConfirmed(updated);
        return toView(incidents.findById(incidentId).orElseThrow(), engineerId, username);
    }

    @Override
    public IncidentView close(Long incidentId, String username, String comment) {
        var engineerId = requireEngineerId(username);
        var current = incidents.findById(incidentId)
                .orElseThrow(() -> new NoSuchElementException("Инцидент не найден: " + incidentId));
        var updated = current.close(engineerId);
        incidents.updateClose(
                updated.id(),
                updated.assignedEngineerId(),
                normalizeComment(comment),
                engineerId
        );
        var persisted = incidents.findById(incidentId).orElseThrow();
        trackerSync.onIncidentClosed(persisted, normalizeComment(comment), username);
        return toView(incidents.findById(incidentId).orElseThrow(), engineerId, username);
    }

    private static String normalizeComment(String comment) {
        return comment == null || comment.isBlank() ? null : comment.trim();
    }

    /** NEW, CONFIRMED и CLOSED (история) видны инженерам в мобильном приложении. */
    private static boolean canUserSee(Incident inc, boolean admin) {
        if (admin) {
            return true;
        }
        return inc.status() == Status.NEW
                || inc.status() == Status.CONFIRMED
                || inc.status() == Status.CLOSED;
    }

    /** Для scope=all: admin — всё; инженер — активные + закрытые (история). */
    private List<Incident> listAllForSync(boolean admin) {
        if (admin) {
            return incidents.findAll();
        }
        var byId = new java.util.LinkedHashMap<Long, Incident>();
        for (var inc : incidents.findActiveForEngineers()) {
            byId.put(inc.id(), inc);
        }
        for (var inc : incidents.findClosed()) {
            byId.putIfAbsent(inc.id(), inc);
        }
        return java.util.List.copyOf(byId.values());
    }

    private Long requireEngineerId(String username) {
        return engineers.findByUsername(username)
                .map(e -> e.id())
                .orElseThrow(() -> new NoSuchElementException("Пользователь не найден: " + username));
    }

    private String resolveAssigneeUsername(Long engineerId) {
        if (engineerId == null) {
            return null;
        }
        return engineers.findById(engineerId).map(e -> e.username()).orElse(null);
    }

    /** Логин для UI и Трекера: tracker_login, иначе username. */
    private String resolveEngineerDisplayLogin(Long engineerId) {
        if (engineerId == null) {
            return null;
        }
        return engineers.findById(engineerId)
                .flatMap(TrackerAssigneeResolver::resolveLogin)
                .orElse(null);
    }

    private IncidentView toView(Incident incident, Long currentEngineerId, String currentUsername) {
        var assigneeUsername = resolveEngineerDisplayLogin(incident.assignedEngineerId());
        var closedByUsername = resolveEngineerDisplayLogin(incident.closedByEngineerId());
        boolean canAccept = incident.status() == Status.NEW;
        boolean canClose = incident.status() == Status.CONFIRMED
                && incident.assignedEngineerId() != null
                && incident.assignedEngineerId().equals(currentEngineerId);

        if (incident.isPrometheusSourced()) {
            var alertName = incident.prometheusAlertName();
            var description = incident.prometheusDescription();
            var severity = incident.prometheusSeverity() != null ? incident.prometheusSeverity() : Severity.HIGH;
            return new IncidentView(
                    incident.id(),
                    incident.ruleId(),
                    alertName,
                    alertName,
                    description,
                    incident.prometheusExpr(),
                    "prometheus",
                    0.0,
                    severity,
                    incident.status(),
                    incident.timestamp(),
                    incident.assignedEngineerId(),
                    assigneeUsername,
                    incident.resolvedAt(),
                    canAccept,
                    canClose,
                    incident.trackerIssueKey(),
                    incident.prometheusAlertActive(),
                    siteProperties.addressOrEmpty(),
                    incident.closeComment(),
                    closedByUsername
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
                null,
                null,
                null,
                operator,
                threshold,
                severity,
                incident.status(),
                incident.timestamp(),
                incident.assignedEngineerId(),
                assigneeUsername,
                incident.resolvedAt(),
                canAccept,
                canClose,
                incident.trackerIssueKey(),
                null,
                siteProperties.addressOrEmpty(),
                incident.closeComment(),
                closedByUsername
        );
    }
}

package com.monitoring.core.application.usecases;

import com.monitoring.core.application.model.PrometheusFiringAlert;
import com.monitoring.core.application.ports.out.IncidentEventNotifier;
import com.monitoring.core.application.ports.out.metrics.PrometheusFiringAlertsReader;
import com.monitoring.core.application.ports.out.repositories.IncidentRepository;
import com.monitoring.core.application.ports.out.repositories.NotificationRepository;
import com.monitoring.core.domain.Incident;
import com.monitoring.core.domain.Notification;
import com.monitoring.core.domain.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Синхронизация инцидентов с firing-алертами Prometheus (правила задаются в Prometheus, не в БД).
 */
public class PrometheusAlertSyncService {

    private static final Logger log = LoggerFactory.getLogger(PrometheusAlertSyncService.class);

    private final PrometheusFiringAlertsReader firingAlerts;
    private final IncidentRepository incidents;
    private final NotificationRepository notifications;
    private final IncidentEventNotifier eventNotifier;
    private final TrackerIncidentSyncService trackerSync;
    private final long defaultNotifyEngineerId;

    public PrometheusAlertSyncService(
            PrometheusFiringAlertsReader firingAlerts,
            IncidentRepository incidents,
            NotificationRepository notifications,
            IncidentEventNotifier eventNotifier,
            TrackerIncidentSyncService trackerSync,
            long defaultNotifyEngineerId
    ) {
        this.firingAlerts = firingAlerts;
        this.incidents = incidents;
        this.notifications = notifications;
        this.eventNotifier = eventNotifier;
        this.trackerSync = trackerSync;
        this.defaultNotifyEngineerId = defaultNotifyEngineerId;
    }

    public int syncFromPrometheus() {
        var firing = firingAlerts.listFiringAlerts();
        Set<String> activeFingerprints = firing.stream()
                .map(PrometheusFiringAlert::fingerprint)
                .collect(Collectors.toCollection(HashSet::new));

        int created = 0;
        for (var alert : firing) {
            markPrometheusAlertActiveIfNeeded(alert.fingerprint());
            if (tryCreateIncident(alert)) {
                created++;
            }
        }

        int resolved = resolveInactivePrometheusIncidents(activeFingerprints);
        if (resolved > 0) {
            log.info("Синхронизация Prometheus: автозакрыто инцидентов {}", resolved);
        }
        if (created > 0) {
            log.info("Синхронизация Prometheus: создано инцидентов {}", created);
        }
        return created;
    }

    private void markPrometheusAlertActiveIfNeeded(String fingerprint) {
        incidents.findOpenByPrometheusFingerprint(fingerprint).ifPresent(open -> {
            if (open.status() == Status.CONFIRMED && Boolean.FALSE.equals(open.prometheusAlertActive())) {
                incidents.updatePrometheusAlertActive(open.id(), true);
                log.debug("Prometheus: алерт снова firing, инцидент id={}", open.id());
            }
        });
    }

    private boolean tryCreateIncident(PrometheusFiringAlert alert) {
        if (incidents.findOpenByPrometheusFingerprint(alert.fingerprint()).isPresent()) {
            return false;
        }
        var incident = incidents.create(Incident.newFromPrometheus(
                alert.fingerprint(),
                alert.alertName(),
                alert.expr(),
                alert.summary(),
                alert.description(),
                alert.severity()
        ));
        var notification = notifications.create(new Notification(
                null,
                incident.id(),
                defaultNotifyEngineerId,
                "push",
                false,
                null
        ));
        var message = IncidentAlertMessages.createdFromPrometheus(alert);
        eventNotifier.incidentCreated(
                incident.id(),
                notification.id(),
                alert.severity(),
                alert.displayTitle(),
                message
        );
        log.info("Инцидент id={} из Prometheus alert={} expr={}", incident.id(), alert.alertName(), alert.expr());
        return true;
    }

    private int resolveInactivePrometheusIncidents(Set<String> activeFingerprints) {
        int autoClosed = 0;
        for (var open : incidents.findAllOpenPrometheusSourced()) {
            if (activeFingerprints.contains(open.prometheusFingerprint())) {
                continue;
            }
            if (open.status() == Status.CONFIRMED) {
                if (!Boolean.FALSE.equals(open.prometheusAlertActive())) {
                    incidents.updatePrometheusAlertActive(open.id(), false);
                    log.info(
                            "Prometheus: алерт погас, инцидент id={} остаётся в работе (prometheusAlertActive=false)",
                            open.id()
                    );
                }
                continue;
            }
            var closed = open.autoResolve();
            incidents.updateStatus(closed.id(), closed.status().name(), closed.assignedEngineerId());
            var metric = open.prometheusAlertName() != null ? open.prometheusAlertName() : open.prometheusExpr();
            eventNotifier.incidentResolved(
                    closed.id(),
                    metric,
                    "Восстановлено: " + open.prometheusAlertName()
            );
            trackerSync.onIncidentClosed(closed, null, null);
            autoClosed++;
        }
        return autoClosed;
    }
}

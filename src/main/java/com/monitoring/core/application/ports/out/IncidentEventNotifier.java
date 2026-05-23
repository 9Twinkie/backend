package com.monitoring.core.application.ports.out;

import com.monitoring.core.domain.Severity;

/**
 * Доставка событий инцидентов клиентам в реальном времени (WebSocket и т.п.).
 */
public interface IncidentEventNotifier {

    void incidentCreated(
            long incidentId,
            long notificationId,
            Severity severity,
            String metricName,
            String message
    );

    void incidentResolved(long incidentId, String metricName, String message);
}

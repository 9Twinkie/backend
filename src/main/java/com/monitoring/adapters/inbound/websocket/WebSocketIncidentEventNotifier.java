package com.monitoring.adapters.inbound.websocket;

import com.monitoring.core.application.ports.out.IncidentEventNotifier;
import com.monitoring.core.application.usecases.IncidentAlertMessages;
import com.monitoring.core.domain.Severity;
import org.springframework.stereotype.Component;

@Component
public class WebSocketIncidentEventNotifier implements IncidentEventNotifier {

    private final NotificationsWebSocketHandler webSocket;

    public WebSocketIncidentEventNotifier(NotificationsWebSocketHandler webSocket) {
        this.webSocket = webSocket;
    }

    @Override
    public void incidentCreated(
            long incidentId,
            long notificationId,
            Severity severity,
            String metricName,
            String message
    ) {
        webSocket.broadcast(new WsOutboundMessage(
                IncidentAlertMessages.eventTypeForSeverity(severity),
                incidentId,
                notificationId,
                severity.name(),
                metricName,
                message
        ));
    }

    @Override
    public void incidentResolved(long incidentId, String metricName, String message) {
        webSocket.broadcast(new WsOutboundMessage(
                "incident_resolved",
                incidentId,
                null,
                null,
                metricName,
                message
        ));
    }
}

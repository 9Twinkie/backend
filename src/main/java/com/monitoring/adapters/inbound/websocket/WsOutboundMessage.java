package com.monitoring.adapters.inbound.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * JSON-сообщение WebSocket для мобильного клиента.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WsOutboundMessage(
        String type,
        Long incidentId,
        Long notificationId,
        String severity,
        String metricName,
        String message
) {
}

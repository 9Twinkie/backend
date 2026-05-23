package com.monitoring.adapters.inbound.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket для push-событий инцидентов. Служебные сообщения (connected, pong) не содержат incidentId.
 */
@Component
public class NotificationsWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationsWebSocketHandler.class);

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public NotificationsWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        sessions.put(session.getId(), session);
        sendJson(session, new WsOutboundMessage("connected", null, null, null, null, null));
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        var payload = message.getPayload().trim();
        if (payload.isEmpty()) {
            return;
        }
        if ("ping".equalsIgnoreCase(payload)) {
            sendJson(session, new WsOutboundMessage("pong", null, null, null, null, null));
            return;
        }
        if (payload.contains("\"type\"") && payload.contains("ping")) {
            sendJson(session, new WsOutboundMessage("pong", null, null, null, null, null));
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        sessions.remove(session.getId());
    }

    public void broadcast(WsOutboundMessage message) {
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException ex) {
            log.error("Не удалось сериализовать WebSocket-сообщение", ex);
            return;
        }
        for (var session : sessions.values()) {
            sendRaw(session, json);
        }
    }

    private void sendJson(WebSocketSession session, WsOutboundMessage message) {
        try {
            sendRaw(session, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException ex) {
            log.warn("Ошибка сериализации WS для сессии {}", session.getId(), ex);
        }
    }

    private void sendRaw(WebSocketSession session, String json) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(json));
        } catch (IOException ex) {
            log.debug("Ошибка отправки WS, закрываем сессию {}", session.getId(), ex);
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (IOException ignored) {
                // ignore
            }
            sessions.remove(session.getId());
        }
    }
}

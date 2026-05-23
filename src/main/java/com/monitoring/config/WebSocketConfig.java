package com.monitoring.config;

import com.monitoring.adapters.inbound.websocket.NotificationsWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Нативный WebSocket без STOMP: endpoint для push-уведомлений.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationsWebSocketHandler notificationsWebSocketHandler;

    public WebSocketConfig(NotificationsWebSocketHandler notificationsWebSocketHandler) {
        this.notificationsWebSocketHandler = notificationsWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationsWebSocketHandler, "/ws/notifications")
                .setAllowedOrigins("*");
    }
}

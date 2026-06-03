package com.monitoring.config;

import com.monitoring.adapters.inbound.websocket.NotificationsWebSocketHandler;
import com.monitoring.adapters.inbound.websocket.terminal.JwtTerminalHandshakeInterceptor;
import com.monitoring.adapters.inbound.websocket.terminal.TerminalWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket: push-уведомления и SSH-терминал.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationsWebSocketHandler notificationsWebSocketHandler;
    private final TerminalWebSocketHandler terminalWebSocketHandler;
    private final JwtTerminalHandshakeInterceptor terminalAuth;

    public WebSocketConfig(
            NotificationsWebSocketHandler notificationsWebSocketHandler,
            TerminalWebSocketHandler terminalWebSocketHandler,
            JwtTerminalHandshakeInterceptor terminalAuth
    ) {
        this.notificationsWebSocketHandler = notificationsWebSocketHandler;
        this.terminalWebSocketHandler = terminalWebSocketHandler;
        this.terminalAuth = terminalAuth;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationsWebSocketHandler, "/ws/notifications")
                .setAllowedOrigins("*");
        registry.addHandler(terminalWebSocketHandler, "/ws/terminal")
                .addInterceptors(terminalAuth)
                .setAllowedOrigins("*");
    }
}

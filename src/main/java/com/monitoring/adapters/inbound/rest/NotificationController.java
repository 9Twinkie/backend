package com.monitoring.adapters.inbound.rest;

import com.monitoring.config.security.SecurityCurrentUser;
import com.monitoring.core.application.model.NotificationView;
import com.monitoring.core.application.usecases.NotificationApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationApplicationService notifications;
    private final SecurityCurrentUser currentUser;

    public NotificationController(NotificationApplicationService notifications, SecurityCurrentUser currentUser) {
        this.notifications = notifications;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<NotificationView> list() {
        return notifications.listForUser(currentUser.username(), currentUser.isAdmin());
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markRead(@PathVariable Long id) {
        boolean updated = notifications.markAsRead(id, currentUser.username(), currentUser.isAdmin());
        return ResponseEntity.ok(Map.of("id", id, "read", updated));
    }
}

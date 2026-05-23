package com.monitoring.core.application.usecases;

import com.monitoring.core.application.model.NotificationView;
import com.monitoring.core.application.ports.in.ListNotificationsUseCase;
import com.monitoring.core.application.ports.in.MarkNotificationReadUseCase;
import com.monitoring.core.application.ports.out.repositories.EngineerRepository;
import com.monitoring.core.application.ports.out.repositories.NotificationRepository;
import com.monitoring.core.domain.Notification;

import java.util.List;
import java.util.NoSuchElementException;

public class NotificationApplicationService implements ListNotificationsUseCase, MarkNotificationReadUseCase {

    private final NotificationRepository notifications;
    private final EngineerRepository engineers;

    public NotificationApplicationService(NotificationRepository notifications, EngineerRepository engineers) {
        this.notifications = notifications;
        this.engineers = engineers;
    }

    @Override
    public List<NotificationView> listForUser(String username, boolean admin) {
        List<Notification> list = admin
                ? notifications.findAll()
                : notifications.findByEngineerId(requireEngineerId(username));
        return list.stream().map(this::toView).toList();
    }

    @Override
    public boolean markAsRead(Long notificationId, String username, boolean admin) {
        var notification = notifications.findById(notificationId)
                .orElseThrow(() -> new NoSuchElementException("Уведомление не найдено: " + notificationId));
        if (!admin) {
            var engineerId = requireEngineerId(username);
            if (!notification.engineerId().equals(engineerId)) {
                throw new IllegalStateException("Нет доступа к уведомлению");
            }
        }
        return notifications.markAsRead(notificationId) > 0;
    }

    private Long requireEngineerId(String username) {
        return engineers.findByUsername(username)
                .map(e -> e.id())
                .orElseThrow(() -> new NoSuchElementException("Пользователь не найден: " + username));
    }

    private NotificationView toView(Notification n) {
        return new NotificationView(
                n.id(),
                n.incidentId(),
                n.engineerId(),
                n.channel(),
                n.delivered(),
                n.readAt(),
                n.readAt() != null
        );
    }
}

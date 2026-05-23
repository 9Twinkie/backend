package com.monitoring.core.application.ports.in;

public interface MarkNotificationReadUseCase {

    boolean markAsRead(Long notificationId, String username, boolean admin);
}

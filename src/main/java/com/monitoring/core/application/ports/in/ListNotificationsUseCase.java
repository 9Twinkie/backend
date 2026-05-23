package com.monitoring.core.application.ports.in;

import com.monitoring.core.application.model.NotificationView;

import java.util.List;

public interface ListNotificationsUseCase {

    List<NotificationView> listForUser(String username, boolean admin);
}

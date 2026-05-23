package com.monitoring.core.application.ports.out.repositories;

import com.monitoring.core.domain.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

    Optional<Notification> findById(Long id);

    List<Notification> findByIncidentId(Long incidentId);

    List<Notification> findAll();

    List<Notification> findByEngineerId(Long engineerId);

    int markAsRead(Long id);

    long countUnreadByEngineer(Long engineerId);

    Notification create(Notification notification);
}

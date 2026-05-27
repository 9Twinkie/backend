package com.monitoring.core.application.ports.out.repositories;

import com.monitoring.core.domain.Incident;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository {

    Optional<Incident> findById(Long id);

    List<Incident> findByEngineerIdAndStatus(Long engineerId, String status);

    List<Incident> findAll();

    /** Активные инциденты для очереди: NEW и CONFIRMED (в работе у любого инженера). */
    List<Incident> findActiveForEngineers();

    /** История: закрытые инциденты. */
    List<Incident> findClosed();

    /** Открытый инцидент по правилу (NEW или CONFIRMED), чтобы не дублировать алерты. */
    Optional<Incident> findOpenByRuleId(Long ruleId);

    Optional<Incident> findOpenByPrometheusFingerprint(String fingerprint);

    /** Все открытые инциденты (NEW или CONFIRMED) для автозакрытия при восстановлении метрики. */
    List<Incident> findAllOpen();

    List<Incident> findAllOpenPrometheusSourced();

    Incident create(Incident incident);

    int updateStatus(Long id, String status, Long engineerId);
}

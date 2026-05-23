package com.monitoring.core.application.ports.out.repositories;

import com.monitoring.core.domain.AlertRule;

import java.util.List;
import java.util.Optional;

public interface AlertRuleRepository {

    Optional<AlertRule> findById(Long id);

    List<AlertRule> findActiveRules();

    AlertRule save(AlertRule rule);

    int updateStatus(Long id, Boolean isActive);
}

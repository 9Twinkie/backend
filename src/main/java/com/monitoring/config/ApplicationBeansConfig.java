package com.monitoring.config;

import com.monitoring.core.application.ports.in.AuthenticateUserUseCase;
import com.monitoring.core.application.ports.out.IncidentEventNotifier;
import com.monitoring.core.application.ports.out.repositories.AlertRuleRepository;
import com.monitoring.core.application.ports.out.repositories.EngineerRepository;
import com.monitoring.core.application.ports.out.repositories.IncidentRepository;
import com.monitoring.core.application.ports.out.repositories.NotificationRepository;
import com.monitoring.core.application.ports.out.security.PasswordHasher;
import com.monitoring.core.application.ports.out.security.PasswordVerifier;
import com.monitoring.core.application.ports.out.security.TokenIssuer;
import com.monitoring.core.application.usecases.UserManagementService;
import com.monitoring.core.application.ports.out.metrics.MetricHistoryReader;
import com.monitoring.core.application.usecases.AuthenticateUserService;
import com.monitoring.core.application.usecases.IncidentApplicationService;
import com.monitoring.core.application.usecases.IncidentChartService;
import com.monitoring.core.application.usecases.NotificationApplicationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Сборка прикладных бинов без Spring-аннотаций внутри домена и сценариев.
 */
@Configuration
public class ApplicationBeansConfig {

    @Bean
    public AuthenticateUserUseCase authenticateUserUseCase(
            EngineerRepository engineers,
            PasswordVerifier passwordVerifier,
            TokenIssuer tokenIssuer
    ) {
        return new AuthenticateUserService(engineers, passwordVerifier, tokenIssuer);
    }

    @Bean
    public IncidentApplicationService incidentApplicationService(
            IncidentRepository incidents,
            AlertRuleRepository alertRules,
            EngineerRepository engineers,
            NotificationRepository notifications,
            IncidentEventNotifier eventNotifier
    ) {
        return new IncidentApplicationService(incidents, alertRules, engineers, notifications, eventNotifier);
    }

    @Bean
    public IncidentChartService incidentChartService(
            com.monitoring.core.application.ports.out.repositories.IncidentRepository incidents,
            com.monitoring.core.application.ports.out.repositories.AlertRuleRepository alertRules,
            com.monitoring.core.application.ports.out.repositories.EngineerRepository engineers,
            MetricHistoryReader metricHistory
    ) {
        return new IncidentChartService(incidents, alertRules, engineers, metricHistory);
    }

    @Bean
    public UserManagementService userManagementService(
            EngineerRepository engineers,
            PasswordHasher passwordHasher
    ) {
        return new UserManagementService(engineers, passwordHasher);
    }

    @Bean
    public NotificationApplicationService notificationApplicationService(
            NotificationRepository notifications,
            EngineerRepository engineers
    ) {
        return new NotificationApplicationService(notifications, engineers);
    }
}

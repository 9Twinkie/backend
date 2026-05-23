package com.monitoring.core.application.ports.in;

import com.monitoring.core.application.model.IncidentView;

import java.util.Optional;

public interface GetIncidentUseCase {

    Optional<IncidentView> getById(Long id, String username, boolean admin);
}

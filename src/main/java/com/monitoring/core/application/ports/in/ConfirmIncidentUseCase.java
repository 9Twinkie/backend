package com.monitoring.core.application.ports.in;

import com.monitoring.core.application.model.IncidentView;

public interface ConfirmIncidentUseCase {

    IncidentView confirm(Long incidentId, String username);
}

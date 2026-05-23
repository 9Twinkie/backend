package com.monitoring.core.application.ports.in;

import com.monitoring.core.application.model.IncidentView;

public interface CloseIncidentUseCase {

    IncidentView close(Long incidentId, String username);
}

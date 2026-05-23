package com.monitoring.core.application.ports.in;

import com.monitoring.core.application.model.IncidentView;

import java.util.List;

public interface ListIncidentsUseCase {

    List<IncidentView> listForUser(String username, boolean admin);
}

package com.monitoring.core.application.ports.out.metrics;

import com.monitoring.core.application.model.PrometheusFiringAlert;

import java.util.List;

public interface PrometheusFiringAlertsReader {

    List<PrometheusFiringAlert> listFiringAlerts();
}

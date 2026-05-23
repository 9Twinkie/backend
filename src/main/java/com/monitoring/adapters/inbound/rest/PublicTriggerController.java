package com.monitoring.adapters.inbound.rest;

import com.monitoring.core.application.model.IncidentView;
import com.monitoring.core.application.usecases.IncidentApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Публичный демо-эндпоинт (полностью вне JWT). URL: GET /trigger-incident
 */
@RestController
public class PublicTriggerController {

    private final IncidentApplicationService incidents;

    public PublicTriggerController(IncidentApplicationService incidents) {
        this.incidents = incidents;
    }

    @GetMapping("/trigger-incident")
    public IncidentView trigger(
            @RequestParam(defaultValue = "2") Long ruleId,
            @RequestParam(defaultValue = "2") Long notifyEngineerId
    ) {
        return incidents.createManual(ruleId, notifyEngineerId, "push");
    }
}

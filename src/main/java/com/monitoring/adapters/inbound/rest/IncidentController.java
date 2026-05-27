package com.monitoring.adapters.inbound.rest;

import com.monitoring.adapters.inbound.rest.dto.CreateIncidentRequest;
import com.monitoring.config.security.SecurityCurrentUser;
import com.monitoring.core.application.model.IncidentView;
import com.monitoring.core.application.model.IncidentChartView;
import com.monitoring.core.application.usecases.IncidentApplicationService;
import com.monitoring.core.application.usecases.IncidentChartService;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/incidents", "/alerts"})
public class IncidentController {

    private final IncidentApplicationService incidents;
    private final IncidentChartService incidentCharts;
    private final SecurityCurrentUser currentUser;

    public IncidentController(
            IncidentApplicationService incidents,
            IncidentChartService incidentCharts,
            SecurityCurrentUser currentUser
    ) {
        this.incidents = incidents;
        this.incidentCharts = incidentCharts;
        this.currentUser = currentUser;
    }

    /**
     * @param scope active (по умолчанию) — NEW + CONFIRMED; history — CLOSED; all — всё (удобно admin)
     */
    @GetMapping
    public List<IncidentView> list(@RequestParam(defaultValue = "active") String scope) {
        return incidents.listForUser(currentUser.username(), currentUser.isAdmin(), scope);
    }

    /** Создать тестовый инцидент. ruleId: 1=cpu HIGH, 2=memory CRITICAL */
    @PostMapping
    public ResponseEntity<IncidentView> create(@Valid @RequestBody CreateIncidentRequest request) {
        var created = incidents.createManual(
                request.ruleId(),
                request.notifyEngineerId(),
                request.channel()
        );
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentView> get(@PathVariable Long id) {
        return incidents.getById(id, currentUser.username(), currentUser.isAdmin())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Точки графика за последние N часов (Prometheus query_range через бэкенд).
     */
    @GetMapping("/{id}/chart")
    public ResponseEntity<IncidentChartView> chart(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int hours,
            @RequestParam(defaultValue = "15s") String step
    ) {
        return incidentCharts.chartForIncident(id, currentUser.username(), currentUser.isAdmin(), hours, step)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/confirm")
    public IncidentView confirm(@PathVariable Long id) {
        return incidents.confirm(id, currentUser.username());
    }

    @PostMapping("/{id}/close")
    public IncidentView close(@PathVariable Long id) {
        return incidents.close(id, currentUser.username());
    }
}

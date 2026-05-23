package com.monitoring.adapters.inbound.rest;

import com.monitoring.adapters.inbound.rest.dto.LoginRequest;
import com.monitoring.adapters.inbound.rest.dto.TokenResponse;
import com.monitoring.adapters.inbound.rest.dto.UserProfileResponse;
import com.monitoring.config.security.SecurityCurrentUser;
import com.monitoring.core.application.model.IncidentView;
import com.monitoring.core.application.ports.in.AuthenticateUserUseCase;
import com.monitoring.core.application.ports.out.repositories.EngineerRepository;
import com.monitoring.core.application.usecases.IncidentApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Публичные и защищённые эндпоинты аутентификации.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final EngineerRepository engineers;
    private final SecurityCurrentUser currentUser;
    private final IncidentApplicationService incidents;

    public AuthController(
            AuthenticateUserUseCase authenticateUserUseCase,
            EngineerRepository engineers,
            SecurityCurrentUser currentUser,
            IncidentApplicationService incidents
    ) {
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.engineers = engineers;
        this.currentUser = currentUser;
        this.incidents = incidents;
    }

    /**
     * Локальный демо-вызов без JWT (путь под /auth/** — всегда разрешён Security).
     * ruleId: 1=cpu HIGH, 2=memory CRITICAL
     */
    @GetMapping("/trigger-incident")
    public IncidentView triggerIncident(
            @RequestParam(defaultValue = "2") Long ruleId,
            @RequestParam(defaultValue = "2") Long notifyEngineerId
    ) {
        return incidents.createManual(ruleId, notifyEngineerId, "push");
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return authenticateUserUseCase.authenticate(request.username(), request.password())
                .map(token -> ResponseEntity.ok(new TokenResponse(token)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me() {
        return engineers.findByUsername(currentUser.username())
                .map(e -> ResponseEntity.ok(new UserProfileResponse(
                        e.id(),
                        e.username(),
                        e.role(),
                        e.phone(),
                        e.notificationPrefs()
                )))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

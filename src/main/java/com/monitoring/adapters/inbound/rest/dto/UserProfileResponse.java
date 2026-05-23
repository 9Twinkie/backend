package com.monitoring.adapters.inbound.rest.dto;

public record UserProfileResponse(
        Long id,
        String username,
        String role,
        String phone,
        String notificationPrefs
) {
}

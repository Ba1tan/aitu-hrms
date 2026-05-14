package kz.aitu.hrms.notification.client.dto;

import java.util.UUID;

public record UserBriefDto(
        UUID userId,
        String email,
        UUID employeeId
) {}

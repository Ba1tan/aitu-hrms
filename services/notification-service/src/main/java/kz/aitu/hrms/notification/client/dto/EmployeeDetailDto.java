package kz.aitu.hrms.notification.client.dto;

import java.util.UUID;

public record EmployeeDetailDto(
        UUID id,
        String firstName,
        String lastName,
        String email,
        UUID departmentId,
        UUID userId
) {}

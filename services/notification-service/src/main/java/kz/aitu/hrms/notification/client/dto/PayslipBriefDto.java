package kz.aitu.hrms.notification.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PayslipBriefDto(
        UUID id,
        UUID employeeId,
        BigDecimal netSalary
) {}

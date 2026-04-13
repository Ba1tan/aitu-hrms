package kz.aitu.hrms.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollJobCompletedEvent {
    private UUID periodId;
    private int employeeCount;
    private BigDecimal totalGross;
    private BigDecimal totalNet;
    private LocalDateTime completedAt;
}

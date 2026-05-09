package kz.aitu.hrms.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Emitted per payslip when ai-ml-service flags it (anomaly_score above the
 * configured threshold). Consumers: notification-service (alerts HR), and
 * reporting-service (anomaly audit log).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollAnomalyDetectedEvent {
    private UUID payslipId;
    private UUID employeeId;
    private UUID periodId;
    private BigDecimal anomalyScore;
    private List<String> flags;
}
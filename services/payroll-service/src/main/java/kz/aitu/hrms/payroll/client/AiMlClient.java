package kz.aitu.hrms.payroll.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * AI/ML service. Anomaly detection during payslip generation is non-critical:
 * if the AI service is unreachable, payslips fall through with anomaly_score
 * left null and status=DRAFT — they can be re-scored later via batch.
 */
@FeignClient(
        name = "ai-ml-service",
        url = "${app.services.ai-ml-service-uri}",
        path = "/v1"
)
public interface AiMlClient {

    @PostMapping("/ai/payroll/detect")
    EmployeeClient.Envelope<AnomalyResponse> detectAnomaly(@RequestBody AnomalyRequest request);

    record AnomalyRequest(
            UUID employeeId,
            UUID periodId,
            BigDecimal grossSalary,
            BigDecimal earnedSalary,
            BigDecimal netSalary,
            BigDecimal allowances,
            BigDecimal deductions,
            BigDecimal opv,
            BigDecimal ipn,
            int workedDays,
            int totalWorkingDays,
            BigDecimal historicalAverage
    ) {}

    record AnomalyResponse(
            boolean anomaly,
            BigDecimal anomalyScore,
            List<String> flags,
            String explanation
    ) {}
}
package kz.aitu.hrms.payroll.service;

import feign.FeignException;
import kz.aitu.hrms.payroll.client.AiMlClient;
import kz.aitu.hrms.payroll.entity.Payslip;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Encapsulates the call to ai-ml-service. Anomaly detection is non-critical:
 * a transport failure leaves the payslip in DRAFT (no flags), so a later
 * batch can score it. This keeps payroll generation resilient to AI outages.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnomalyDetector {

    private final AiMlClient aiClient;

    public Result detect(Payslip payslip, BigDecimal historicalAverage) {
        try {
            AiMlClient.AnomalyRequest req = new AiMlClient.AnomalyRequest(
                    payslip.getEmployeeId(),
                    payslip.getPeriod().getId(),
                    payslip.getGrossSalary(),
                    payslip.getEarnedSalary(),
                    payslip.getNetSalary(),
                    payslip.getAllowances(),
                    payslip.getOtherDeductions(),
                    payslip.getOpvAmount(),
                    payslip.getIpnAmount(),
                    payslip.getWorkedDays(),
                    payslip.getTotalWorkingDays(),
                    historicalAverage
            );
            var resp = aiClient.detectAnomaly(req);
            if (resp == null || resp.data() == null) {
                return Result.unavailable();
            }
            AiMlClient.AnomalyResponse a = resp.data();
            return new Result(true, a.anomaly(), a.anomalyScore(), a.flags());
        } catch (FeignException e) {
            log.debug("ai-ml detect transport error for payslip {}: {}",
                    payslip.getId(), e.getMessage());
        } catch (Exception e) {
            log.warn("ai-ml detect unexpected error for payslip {}: {}",
                    payslip.getId(), e.getMessage());
        }
        return Result.unavailable();
    }

    public record Result(boolean available, boolean anomaly, BigDecimal score, List<String> flags) {
        public static Result unavailable() {
            return new Result(false, false, null, List.of());
        }
    }
}
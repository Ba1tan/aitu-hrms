package kz.aitu.hrms.payroll.service;

import kz.aitu.hrms.common.event.PayrollAnomalyDetectedEvent;
import kz.aitu.hrms.common.event.PayrollJobCompletedEvent;
import kz.aitu.hrms.common.event.PayrollJobStartedEvent;
import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.payroll.calculator.EmployeePayrollProfile;
import kz.aitu.hrms.payroll.client.EmployeeClient;
import kz.aitu.hrms.payroll.dto.PeriodDtos;
import kz.aitu.hrms.payroll.entity.PayrollPeriod;
import kz.aitu.hrms.payroll.entity.PayrollPeriodStatus;
import kz.aitu.hrms.payroll.entity.Payslip;
import kz.aitu.hrms.payroll.entity.PayslipStatus;
import kz.aitu.hrms.payroll.repository.PayrollPeriodRepository;
import kz.aitu.hrms.payroll.repository.PayslipRepository;
import kz.aitu.hrms.payroll.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Synchronously generates payslips for a payroll period. For large companies
 * the controller may dispatch this through Spring Batch instead — see
 * {@code PayrollBatchService}. The two implementations call this service's
 * {@link #generateForEmployee} so the per-employee logic stays unified.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayslipGenerationService {

    private final PayrollPeriodRepository periodRepo;
    private final PayslipRepository payslipRepo;
    private final PayslipFactory payslipFactory;
    private final AnomalyDetector anomalyDetector;
    private final EmployeeLookup employeeLookup;
    private final EventPublisher events;

    @Value("${app.payroll.batch-threshold}")
    private int batchThreshold;

    /** Decide whether to run a generation job inline or through Spring Batch. */
    public boolean shouldUseBatch(int employeeCount, Boolean asyncOverride) {
        if (asyncOverride != null) return asyncOverride;
        return employeeCount > batchThreshold;
    }

    public List<EmployeeClient.EmployeeSummary> resolveEmployees(PeriodDtos.GenerateRequest req) {
        List<EmployeeClient.EmployeeSummary> active = employeeLookup.listActive();
        if (active.isEmpty()) {
            throw new BusinessException(
                    "No active employees returned by employee-service — cannot generate payslips.");
        }
        if (req != null && req.getEmployeeIds() != null && !req.getEmployeeIds().isEmpty()) {
            Set<UUID> filter = new HashSet<>(req.getEmployeeIds());
            return active.stream().filter(e -> filter.contains(e.id())).collect(Collectors.toList());
        }
        return active;
    }

    /**
     * Inline generation path. Wrapped in a single transaction so a partial
     * failure rolls back unfinished payslips while DRAFT payslips already
     * persisted by an earlier successful run are preserved by skipping
     * existing rows.
     */
    @Transactional
    public PeriodDtos.GenerateResponse generate(UUID periodId, PeriodDtos.GenerateRequest req) {
        PayrollPeriod period = requireGenerablePeriod(periodId);
        List<EmployeeClient.EmployeeSummary> employees = resolveEmployees(req);

        UUID actor = CurrentUser.userId();
        events.publishJobStarted(PayrollJobStartedEvent.builder()
                .periodId(period.getId())
                .year(period.getYear())
                .month(period.getMonth())
                .employeeCount(employees.size())
                .startedAt(LocalDateTime.now())
                .startedBy(actor)
                .build());

        if (period.getStatus() == PayrollPeriodStatus.DRAFT) {
            period.setStatus(PayrollPeriodStatus.PROCESSING);
            period.setProcessedBy(actor);
            period.setProcessedAt(LocalDateTime.now());
            periodRepo.save(period);
        }

        int generated = 0, skipped = 0, errors = 0, flagged = 0;
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        List<String> errorDetails = new ArrayList<>();

        for (EmployeeClient.EmployeeSummary summary : employees) {
            GenerationOutcome outcome = generateForEmployee(period, summary.id(), summary.fullName());
            switch (outcome.status()) {
                case GENERATED -> {
                    generated++;
                    totalGross = totalGross.add(outcome.gross());
                    totalNet = totalNet.add(outcome.net());
                    if (outcome.flagged()) flagged++;
                }
                case SKIPPED -> skipped++;
                case ERROR -> {
                    errors++;
                    errorDetails.add(outcome.errorMessage());
                }
            }
        }

        period.setStatus(PayrollPeriodStatus.COMPLETED);
        periodRepo.save(period);
        log.info("Payslip generation finished for {}: generated={}, skipped={}, errors={}, flagged={}",
                period.getName(), generated, skipped, errors, flagged);

        events.publishJobCompleted(PayrollJobCompletedEvent.builder()
                .periodId(period.getId())
                .employeeCount(generated)
                .totalGross(totalGross)
                .totalNet(totalNet)
                .completedAt(LocalDateTime.now())
                .build());

        return PeriodDtos.GenerateResponse.builder()
                .async(false)
                .generated(generated)
                .skipped(skipped)
                .errors(errors)
                .flagged(flagged)
                .totalGrossPayout(totalGross)
                .totalNetPayout(totalNet)
                .errorDetails(errorDetails)
                .build();
    }

    /**
     * Per-employee unit of work used by both inline and batch paths. Idempotent
     * via {@code existsByPeriodIdAndEmployeeIdAndDeletedFalse}.
     */
    @Transactional
    public GenerationOutcome generateForEmployee(PayrollPeriod period, UUID employeeId, String displayName) {
        try {
            if (payslipRepo.existsByPeriodIdAndEmployeeIdAndDeletedFalse(period.getId(), employeeId)) {
                return GenerationOutcome.skipped();
            }
            EmployeePayrollProfile profile = employeeLookup.profileOrThrow(employeeId);
            if (profile.getBaseSalary() == null
                    || profile.getBaseSalary().compareTo(BigDecimal.ZERO) <= 0) {
                return GenerationOutcome.error(
                        "Skip " + displayName + ": no base salary on file");
            }

            Payslip payslip = payslipFactory.build(profile, period);
            payslip = payslipRepo.save(payslip);

            // AI anomaly detection — non-critical
            AnomalyDetector.Result ai = anomalyDetector.detect(payslip, profile.getBaseSalary());
            boolean flagged = false;
            if (ai.available()) {
                payslip.setAnomalyScore(ai.score());
                if (ai.flags() != null && !ai.flags().isEmpty()) {
                    payslip.setAnomalyFlags(String.join(",", ai.flags()));
                }
                if (ai.score() != null
                        && ai.score().compareTo(payslipFactory.getAnomalyFlagThreshold()) > 0) {
                    payslip.setStatus(PayslipStatus.FLAGGED);
                    flagged = true;
                    events.publishAnomaly(PayrollAnomalyDetectedEvent.builder()
                            .payslipId(payslip.getId())
                            .employeeId(payslip.getEmployeeId())
                            .periodId(period.getId())
                            .anomalyScore(ai.score())
                            .flags(ai.flags())
                            .build());
                }
                payslipRepo.save(payslip);
            }
            return GenerationOutcome.generated(payslip.getGrossSalary(), payslip.getNetSalary(), flagged);
        } catch (BusinessException be) {
            return GenerationOutcome.error("Skip " + displayName + ": " + be.getMessage());
        } catch (Exception ex) {
            log.error("Failed to generate payslip for {} ({})", displayName, employeeId, ex);
            return GenerationOutcome.error("Error " + displayName + ": " + ex.getMessage());
        }
    }

    PayrollPeriod requireGenerablePeriod(UUID periodId) {
        PayrollPeriod period = periodRepo.findByIdAndDeletedFalse(periodId)
                .orElseThrow(() -> new BusinessException("Payroll period not found: " + periodId));
        if (period.getStatus() == PayrollPeriodStatus.LOCKED
                || period.getStatus() == PayrollPeriodStatus.PAID
                || period.getStatus() == PayrollPeriodStatus.APPROVED) {
            throw new BusinessException(
                    "Cannot generate payslips for period in status " + period.getStatus());
        }
        return period;
    }

    public enum Status { GENERATED, SKIPPED, ERROR }

    public record GenerationOutcome(Status status,
                                    BigDecimal gross,
                                    BigDecimal net,
                                    boolean flagged,
                                    String errorMessage) {
        public static GenerationOutcome generated(BigDecimal g, BigDecimal n, boolean flagged) {
            return new GenerationOutcome(Status.GENERATED, g, n, flagged, null);
        }
        public static GenerationOutcome skipped() {
            return new GenerationOutcome(Status.SKIPPED, BigDecimal.ZERO, BigDecimal.ZERO, false, null);
        }
        public static GenerationOutcome error(String message) {
            return new GenerationOutcome(Status.ERROR, BigDecimal.ZERO, BigDecimal.ZERO, false, message);
        }
    }
}
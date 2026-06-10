package kz.aitu.hrms.integration.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.integration.client.EmployeeClient;
import kz.aitu.hrms.integration.client.PayrollClient;
import kz.aitu.hrms.integration.client.dto.EmployeeIinDto;
import kz.aitu.hrms.integration.client.dto.PayslipDetailDto;
import kz.aitu.hrms.integration.domain.SyncJob;
import kz.aitu.hrms.integration.domain.SyncStatus;
import kz.aitu.hrms.integration.domain.SyncTarget;
import kz.aitu.hrms.integration.dto.mapper.SyncJobMapper;
import kz.aitu.hrms.integration.dto.sync.SyncJobDto;
import kz.aitu.hrms.integration.dto.sync.SyncTriggerResponseDto;
import kz.aitu.hrms.integration.event.publisher.IntegrationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncOrchestrator {

    private final SyncJobService jobService;
    private final PayrollClient payrollClient;
    private final EmployeeClient employeeClient;
    private final OneCPayloadBuilder payloadBuilder;
    private final OneCClient oneCClient;
    private final SettingsService settingsService;
    private final IntegrationEventPublisher eventPublisher;
    private final SyncJobMapper syncJobMapper;
    private final AuditPublisher auditPublisher;

    @Transactional
    public SyncTriggerResponseDto trigger(UUID periodId, UUID triggeredBy) {
        String baseUrl = settingsService.getOrDefault("integration.1c_base_url", "");
        if (baseUrl.isBlank()) {
            log.info("1C sync skipped — integration.1c_base_url not configured for period {}", periodId);
            return SyncTriggerResponseDto.skipped(periodId);
        }

        if (jobService.hasActiveOrSuccessJob(periodId, SyncTarget.ONE_C)) {
            throw new BusinessException("Sync already in progress or completed for period " + periodId);
        }

        SyncJob job = jobService.create(periodId, SyncTarget.ONE_C, triggeredBy.toString());
        performSync(job);
        auditPublisher.audit("SYNC", "SYNC_JOB", job.getId(),
                null, java.util.Map.of("periodId", periodId, "target", "ONE_C",
                        "status", String.valueOf(job.getStatus())));
        return SyncTriggerResponseDto.from(job);
    }

    @Transactional
    public SyncJobDto retryJob(UUID jobId, UUID userId) {
        SyncJob job = jobService.getEntityById(jobId);
        if (job.getStatus() != SyncStatus.FAILED && job.getStatus() != SyncStatus.RETRYING) {
            throw new BusinessException("Only FAILED or RETRYING jobs can be retried manually");
        }
        performSync(job);
        auditPublisher.audit("RETRY", "SYNC_JOB", job.getId(),
                null, java.util.Map.of("periodId", job.getPeriodId(),
                        "status", String.valueOf(job.getStatus()),
                        "retryCount", job.getRetryCount()));
        return syncJobMapper.toDto(job);
    }

    public void performSync(SyncJob job) {
        try {
            jobService.transitionTo(job, SyncStatus.IN_PROGRESS);

            List<PayslipDetailDto> payslips = payrollClient
                    .listPayslipsForPeriod(job.getPeriodId(), 0, 1000)
                    .getContent();

            List<UUID> employeeIds = payslips.stream()
                    .map(PayslipDetailDto::getEmployeeId)
                    .distinct()
                    .collect(Collectors.toList());

            // TODO(employee-service): replace with bulk call once endpoint exists
            List<EmployeeIinDto> employees = employeeIds.parallelStream()
                    .map(id -> {
                        try {
                            return employeeClient.getById(id);
                        } catch (Exception e) {
                            log.warn("Failed to fetch IIN for employee {}: {}", id, e.getMessage());
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());

            String payload = payloadBuilder.build(job.getPeriodId(), payslips, employees);
            job.setPayload(payload);

            OneCResponse result = oneCClient.sendPayroll(payload);

            job.setOnecDocumentId(result.getDocumentId());
            job.setResponse(result.toJson());
            jobService.transitionTo(job, SyncStatus.SUCCESS);

            eventPublisher.publishCompleted(job);
            log.info("Sync SUCCESS — job={}, period={}, doc={}",
                    job.getId(), job.getPeriodId(), job.getOnecDocumentId());

        } catch (Exception e) {
            handleFailure(job, e);
        }
    }

    private void handleFailure(SyncJob job, Exception e) {
        job.setErrorMessage(e.getMessage());
        job.incrementRetryCount();

        if (job.getRetryCount() < job.getMaxRetries()) {
            job.setNextRetryAt(job.computeNextRetryAt());
            jobService.transitionTo(job, SyncStatus.RETRYING);
        } else {
            jobService.transitionTo(job, SyncStatus.FAILED);
        }

        eventPublisher.publishFailed(job);
        log.error("Sync FAILED — job={}, retry={}/{}, error={}",
                job.getId(), job.getRetryCount(), job.getMaxRetries(), e.getMessage());
    }
}

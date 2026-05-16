package kz.aitu.hrms.integration.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.integration.client.EmployeeClient;
import kz.aitu.hrms.integration.client.PayrollClient;
import kz.aitu.hrms.integration.client.dto.PageResponse;
import kz.aitu.hrms.integration.client.dto.PayslipDetailDto;
import kz.aitu.hrms.integration.domain.SyncJob;
import kz.aitu.hrms.integration.domain.SyncStatus;
import kz.aitu.hrms.integration.domain.SyncTarget;
import kz.aitu.hrms.integration.dto.mapper.SyncJobMapper;
import kz.aitu.hrms.integration.dto.sync.SyncJobDto;
import kz.aitu.hrms.integration.dto.sync.SyncTriggerResponseDto;
import kz.aitu.hrms.integration.event.publisher.IntegrationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncOrchestratorTest {

    @Mock private SyncJobService jobService;
    @Mock private PayrollClient payrollClient;
    @Mock private EmployeeClient employeeClient;
    @Mock private OneCPayloadBuilder payloadBuilder;
    @Mock private OneCClient oneCClient;
    @Mock private SettingsService settingsService;
    @Mock private IntegrationEventPublisher eventPublisher;
    @Mock private SyncJobMapper syncJobMapper;
    @InjectMocks private SyncOrchestrator orchestrator;

    private UUID periodId;
    private UUID userId;
    private SyncJob syncJob;

    @BeforeEach
    void setup() {
        periodId = UUID.randomUUID();
        userId   = UUID.randomUUID();
        syncJob  = SyncJob.builder()
                .id(UUID.randomUUID())
                .periodId(periodId)
                .target(SyncTarget.ONE_C)
                .status(SyncStatus.PENDING)
                .retryCount(0)
                .maxRetries(3)
                .build();
    }

    @Test
    void trigger_1cBlankUrl_returnsSkipped() {
        when(settingsService.getOrDefault("integration.1c_base_url", "")).thenReturn("");

        SyncTriggerResponseDto result = orchestrator.trigger(periodId, userId);

        assertThat(result.getMessage()).contains("Sync skipped");
        verify(jobService, never()).create(any(), any(), any());
    }

    @Test
    void trigger_alreadyRunning_throwsBusinessException() {
        when(settingsService.getOrDefault("integration.1c_base_url", "")).thenReturn("http://1c.example.com");
        when(jobService.hasActiveOrSuccessJob(periodId, SyncTarget.ONE_C)).thenReturn(true);

        assertThatThrownBy(() -> orchestrator.trigger(periodId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already in progress");
    }

    @Test
    void trigger_happyPath_successStatus() {
        when(settingsService.getOrDefault("integration.1c_base_url", "")).thenReturn("http://1c.example.com");
        when(jobService.hasActiveOrSuccessJob(periodId, SyncTarget.ONE_C)).thenReturn(false);
        when(jobService.create(any(), any(), any())).thenReturn(syncJob);

        PageResponse<PayslipDetailDto> page = new PageResponse<>();
        page.setContent(List.of());
        when(payrollClient.listPayslipsForPeriod(any(), anyInt(), anyInt())).thenReturn(page);
        when(payloadBuilder.build(any(), any(), any())).thenReturn("{}");

        OneCResponse response = new OneCResponse();
        response.setDocumentId("DOC-001");
        when(oneCClient.sendPayroll(any())).thenReturn(response);

        orchestrator.trigger(periodId, userId);

        verify(jobService).transitionTo(syncJob, SyncStatus.IN_PROGRESS);
        verify(jobService).transitionTo(syncJob, SyncStatus.SUCCESS);
        verify(eventPublisher).publishCompleted(syncJob);
    }

    @Test
    void performSync_failure_belowMaxRetries_setsRetrying() {
        syncJob.setRetryCount(0);
        when(payrollClient.listPayslipsForPeriod(any(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Connection refused"));

        orchestrator.performSync(syncJob);

        verify(jobService).transitionTo(syncJob, SyncStatus.RETRYING);
        verify(eventPublisher).publishFailed(syncJob);
        assertThat(syncJob.getNextRetryAt()).isNotNull();
    }

    @Test
    void performSync_failure_atMaxRetries_setsFailed() {
        syncJob.setRetryCount(2);
        syncJob.setMaxRetries(3);
        when(payrollClient.listPayslipsForPeriod(any(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("1C down"));

        orchestrator.performSync(syncJob);

        verify(jobService).transitionTo(syncJob, SyncStatus.FAILED);
        verify(eventPublisher).publishFailed(syncJob);
    }

    @Test
    void retryJob_onlyAllowedForFailedOrRetrying() {
        syncJob.setStatus(SyncStatus.SUCCESS);
        when(jobService.getEntityById(syncJob.getId())).thenReturn(syncJob);

        assertThatThrownBy(() -> orchestrator.retryJob(syncJob.getId(), userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("FAILED or RETRYING");
    }

    @Test
    void retryJob_failedJob_performsSync() {
        syncJob.setStatus(SyncStatus.FAILED);
        when(jobService.getEntityById(syncJob.getId())).thenReturn(syncJob);
        when(syncJobMapper.toDto(any())).thenReturn(new SyncJobDto());

        PageResponse<PayslipDetailDto> page = new PageResponse<>();
        page.setContent(List.of());
        when(payrollClient.listPayslipsForPeriod(any(), anyInt(), anyInt())).thenReturn(page);
        when(payloadBuilder.build(any(), any(), any())).thenReturn("{}");
        OneCResponse resp = new OneCResponse();
        resp.setDocumentId("DOC-RETRY");
        when(oneCClient.sendPayroll(any())).thenReturn(resp);

        SyncJobDto result = orchestrator.retryJob(syncJob.getId(), userId);

        assertThat(result).isNotNull();
        verify(jobService).transitionTo(syncJob, SyncStatus.SUCCESS);
    }
}

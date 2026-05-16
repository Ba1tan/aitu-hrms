package kz.aitu.hrms.integration.service;

import kz.aitu.hrms.integration.domain.SyncJob;
import kz.aitu.hrms.integration.domain.SyncStatus;
import kz.aitu.hrms.integration.domain.SyncTarget;
import kz.aitu.hrms.integration.repository.SyncJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetrySchedulerTest {

    @Mock private SyncJobRepository jobRepo;
    @Mock private SyncOrchestrator orchestrator;
    @InjectMocks private RetryScheduler scheduler;

    @Test
    void processRetries_noDueJobs_doesNothing() {
        when(jobRepo.findDueRetries(any())).thenReturn(List.of());

        scheduler.processRetries();

        verify(orchestrator, never()).performSync(any());
    }

    @Test
    void processRetries_dueJob_callsPerformSync() {
        SyncJob job = SyncJob.builder()
                .id(UUID.randomUUID())
                .periodId(UUID.randomUUID())
                .target(SyncTarget.ONE_C)
                .status(SyncStatus.RETRYING)
                .nextRetryAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(jobRepo.findDueRetries(any())).thenReturn(List.of(job));

        scheduler.processRetries();

        verify(orchestrator).performSync(job);
    }

    @Test
    void processRetries_exceptionSwallowed_continuesOtherJobs() {
        SyncJob job1 = SyncJob.builder()
                .id(UUID.randomUUID()).periodId(UUID.randomUUID())
                .target(SyncTarget.ONE_C).status(SyncStatus.RETRYING).build();
        SyncJob job2 = SyncJob.builder()
                .id(UUID.randomUUID()).periodId(UUID.randomUUID())
                .target(SyncTarget.ONE_C).status(SyncStatus.RETRYING).build();
        when(jobRepo.findDueRetries(any())).thenReturn(List.of(job1, job2));
        doThrow(new RuntimeException("unexpected")).when(orchestrator).performSync(job1);

        scheduler.processRetries();

        verify(orchestrator).performSync(job1);
        verify(orchestrator).performSync(job2);
    }
}

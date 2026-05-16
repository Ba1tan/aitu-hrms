package kz.aitu.hrms.integration.service;

import kz.aitu.hrms.integration.domain.SyncJob;
import kz.aitu.hrms.integration.repository.SyncJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryScheduler {

    private final SyncJobRepository jobRepo;
    private final SyncOrchestrator orchestrator;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void processRetries() {
        List<SyncJob> due = jobRepo.findDueRetries(LocalDateTime.now());
        if (due.isEmpty()) return;

        log.info("RetryScheduler: {} jobs due for retry", due.size());
        for (SyncJob job : due) {
            try {
                orchestrator.performSync(job);
            } catch (Exception e) {
                log.error("RetryScheduler: unexpected error for job {}: {}", job.getId(), e.getMessage());
            }
        }
    }
}

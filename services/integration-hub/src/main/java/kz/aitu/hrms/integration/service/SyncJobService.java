package kz.aitu.hrms.integration.service;

import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.integration.domain.SyncJob;
import kz.aitu.hrms.integration.domain.SyncStatus;
import kz.aitu.hrms.integration.domain.SyncTarget;
import kz.aitu.hrms.integration.dto.mapper.SyncJobMapper;
import kz.aitu.hrms.integration.dto.sync.SyncJobDto;
import kz.aitu.hrms.integration.repository.SyncJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SyncJobService {

    private final SyncJobRepository repo;
    private final SyncJobMapper mapper;

    @Transactional(readOnly = true)
    public SyncJobDto getById(UUID id) {
        return mapper.toDto(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public SyncJob getEntityById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SyncJob", id));
    }

    @Transactional(readOnly = true)
    public Page<SyncJobDto> list(SyncTarget target, SyncStatus status, Pageable pageable) {
        return repo.findFiltered(target, status, pageable).map(mapper::toDto);
    }

    public SyncJob create(UUID periodId, SyncTarget target, String createdBy) {
        SyncJob job = SyncJob.builder()
                .periodId(periodId)
                .target(target)
                .status(SyncStatus.PENDING)
                .createdBy(createdBy)
                .build();
        return repo.save(job);
    }

    public void transitionTo(SyncJob job, SyncStatus newStatus) {
        job.setStatus(newStatus);
        if (newStatus == SyncStatus.SUCCESS || newStatus == SyncStatus.FAILED) {
            job.setCompletedAt(java.time.LocalDateTime.now());
        }
        repo.save(job);
    }

    public boolean hasActiveOrSuccessJob(UUID periodId, SyncTarget target) {
        return repo.existsByPeriodIdAndTargetAndStatusInAndDeletedFalse(
                periodId, target,
                List.of(SyncStatus.IN_PROGRESS, SyncStatus.SUCCESS));
    }
}

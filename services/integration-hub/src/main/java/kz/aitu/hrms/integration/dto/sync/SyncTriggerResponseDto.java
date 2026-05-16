package kz.aitu.hrms.integration.dto.sync;

import kz.aitu.hrms.integration.domain.SyncJob;
import kz.aitu.hrms.integration.domain.SyncStatus;
import kz.aitu.hrms.integration.domain.SyncTarget;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class SyncTriggerResponseDto {
    private UUID jobId;
    private UUID periodId;
    private SyncTarget target;
    private SyncStatus status;
    private String message;

    public static SyncTriggerResponseDto from(SyncJob job) {
        return SyncTriggerResponseDto.builder()
                .jobId(job.getId())
                .periodId(job.getPeriodId())
                .target(job.getTarget())
                .status(job.getStatus())
                .message("Sync job created with status " + job.getStatus())
                .build();
    }

    public static SyncTriggerResponseDto skipped(UUID periodId) {
        return SyncTriggerResponseDto.builder()
                .periodId(periodId)
                .target(SyncTarget.ONE_C)
                .status(SyncStatus.PENDING)
                .message("Sync skipped — integration.1c_base_url not configured")
                .build();
    }
}

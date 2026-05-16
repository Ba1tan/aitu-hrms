package kz.aitu.hrms.integration.dto.sync;

import kz.aitu.hrms.integration.domain.SyncStatus;
import kz.aitu.hrms.integration.domain.SyncTarget;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class SyncJobDto {
    private UUID id;
    private UUID periodId;
    private SyncTarget target;
    private SyncStatus status;
    private String onecDocumentId;
    private String errorMessage;
    private int retryCount;
    private int maxRetries;
    private LocalDateTime nextRetryAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

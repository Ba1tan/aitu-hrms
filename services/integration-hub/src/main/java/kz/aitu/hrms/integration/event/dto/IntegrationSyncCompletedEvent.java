package kz.aitu.hrms.integration.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * TODO(hrms-common): move to kz.aitu.hrms.common.event when notification-service consumes it
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class IntegrationSyncCompletedEvent {
    private UUID jobId;
    private UUID periodId;
    private String onecDocumentId;
}

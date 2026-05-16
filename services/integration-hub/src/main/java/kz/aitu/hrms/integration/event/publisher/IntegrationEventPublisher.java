package kz.aitu.hrms.integration.event.publisher;

import kz.aitu.hrms.integration.config.RabbitConfig;
import kz.aitu.hrms.integration.domain.SyncJob;
import kz.aitu.hrms.integration.event.dto.IntegrationSyncCompletedEvent;
import kz.aitu.hrms.integration.event.dto.IntegrationSyncFailedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IntegrationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishCompleted(SyncJob job) {
        var event = IntegrationSyncCompletedEvent.builder()
                .jobId(job.getId())
                .periodId(job.getPeriodId())
                .onecDocumentId(job.getOnecDocumentId())
                .build();
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_SYNC_COMPLETED, event);
    }

    public void publishFailed(SyncJob job) {
        var event = IntegrationSyncFailedEvent.builder()
                .jobId(job.getId())
                .periodId(job.getPeriodId())
                .errorMessage(job.getErrorMessage())
                .retryCount(job.getRetryCount())
                .build();
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_SYNC_FAILED, event);
    }
}

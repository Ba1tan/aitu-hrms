package kz.aitu.hrms.integration.listener;

import kz.aitu.hrms.common.event.PayrollJobCompletedEvent;
import kz.aitu.hrms.common.event.PayrollPeriodApprovedEvent;
import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.integration.config.RabbitConfig;
import kz.aitu.hrms.integration.service.SettingsService;
import kz.aitu.hrms.integration.service.SyncOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayrollEventListener {

    private final SettingsService settings;
    private final SyncOrchestrator orchestrator;

    @RabbitListener(queues = RabbitConfig.Q_PAYROLL_PERIOD_APPROVED)
    public void onPayrollPeriodApproved(PayrollPeriodApprovedEvent event) {
        try {
            String baseUrl = settings.getOrDefault("integration.1c_base_url", "");
            if (baseUrl.isBlank()) {
                log.info("1C sync disabled (integration.1c_base_url not set) — skipping period {}",
                        event.getPeriodId());
                return;
            }
            orchestrator.trigger(event.getPeriodId(), event.getApprovedBy());
        } catch (BusinessException e) {
            log.info("1C sync skipped: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to process PayrollPeriodApprovedEvent {}: {}",
                    event.getPeriodId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitConfig.Q_PAYROLL_JOB_COMPLETED)
    public void onPayrollJobCompleted(PayrollJobCompletedEvent event) {
        try {
            String autoSync = settings.getOrDefault("integration.auto_sync_on_complete", "false");
            if ("true".equalsIgnoreCase(autoSync)) {
                // TODO: auto-trigger — only if setting explicitly enabled
            }
            log.debug("PayrollJobCompletedEvent received for period {}; auto_sync={}",
                    event.getPeriodId(), autoSync);
        } catch (Exception e) {
            log.error("Failed to process PayrollJobCompletedEvent: {}", e.getMessage(), e);
        }
    }
}

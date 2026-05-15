package kz.aitu.hrms.reporting.listener;

import kz.aitu.hrms.common.event.PayrollJobCompletedEvent;
import kz.aitu.hrms.reporting.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PayrollEventListener {

    private final CacheManager cacheManager;

    @RabbitListener(queues = RabbitConfig.Q_PAYROLL_JOB_COMPLETED)
    public void onPayrollJobCompleted(PayrollJobCompletedEvent event) {
        try {
            Cache cache = cacheManager.getCache("dashboard");
            if (cache != null) {
                cache.clear();
            }
            log.info("Dashboard cache invalidated after payroll job {} completed", event.getPeriodId());
        } catch (Exception e) {
            log.error("Failed to process PayrollJobCompletedEvent {}: {}", event.getPeriodId(), e.getMessage(), e);
        }
    }
}

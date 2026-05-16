package kz.aitu.hrms.notification.listener;

import kz.aitu.hrms.common.event.PayrollAnomalyDetectedEvent;
import kz.aitu.hrms.common.event.PayrollJobCompletedEvent;
import kz.aitu.hrms.common.event.PayrollJobStartedEvent;
import kz.aitu.hrms.common.event.PayrollPeriodApprovedEvent;
import kz.aitu.hrms.notification.client.PayrollClient;
import kz.aitu.hrms.notification.client.dto.PayslipBriefDto;
import kz.aitu.hrms.notification.config.RabbitConfig;
import kz.aitu.hrms.notification.service.NotificationFactory;
import kz.aitu.hrms.notification.service.NotificationService;
import kz.aitu.hrms.notification.service.RecipientResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayrollEventListener {

    private final NotificationFactory factory;
    private final NotificationService service;
    private final RecipientResolver recipients;
    private final PayrollClient payrollClient;

    @RabbitListener(queues = RabbitConfig.Q_PAYROLL_JOB_STARTED)
    public void onPayrollJobStarted(PayrollJobStartedEvent event) {
        try {
            List<UUID> userIds = recipients.resolveUserIdsByPermission("PAYROLL_PROCESS");
            for (UUID userId : userIds) {
                var built = factory.fromPayrollJobStarted(event, userId);
                service.create(built.notification(), built.idempotencyKey(), built.emailRequest());
            }
            log.info("consumed PayrollJobStartedEvent periodId={}", event.getPeriodId());
        } catch (Exception e) {
            log.error("Failed to process PayrollJobStartedEvent {}: {}", event.getPeriodId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitConfig.Q_PAYROLL_JOB_COMPLETED)
    public void onPayrollJobCompleted(PayrollJobCompletedEvent event) {
        try {
            List<UUID> userIds = recipients.resolveUserIdsByPermission("PAYROLL_PROCESS");
            for (UUID userId : userIds) {
                var built = factory.fromPayrollJobCompleted(event, userId);
                service.create(built.notification(), built.idempotencyKey(), built.emailRequest());
            }
            log.info("consumed PayrollJobCompletedEvent periodId={}", event.getPeriodId());
        } catch (Exception e) {
            log.error("Failed to process PayrollJobCompletedEvent {}: {}", event.getPeriodId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitConfig.Q_PAYROLL_ANOMALY)
    public void onPayrollAnomalyDetected(PayrollAnomalyDetectedEvent event) {
        try {
            List<UUID> userIds = recipients.resolveUserIdsByPermission("PAYROLL_APPROVE");
            for (UUID userId : userIds) {
                var built = factory.fromPayrollAnomalyDetected(event, userId);
                service.create(built.notification(), built.idempotencyKey(), built.emailRequest());
            }
            log.info("consumed PayrollAnomalyDetectedEvent payslipId={}", event.getPayslipId());
        } catch (Exception e) {
            log.error("Failed to process PayrollAnomalyDetectedEvent {}: {}", event.getPayslipId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitConfig.Q_PAYROLL_PERIOD_APPROVED)
    public void onPayrollPeriodApproved(PayrollPeriodApprovedEvent event) {
        try {
            int page = 0;
            Page<PayslipBriefDto> slips;
            do {
                slips = payrollClient.listPayslips(event.getPeriodId(), page, 100);
                for (PayslipBriefDto slip : slips.getContent()) {
                    List<UUID> userIds = recipients.resolveUserIds(slip.employeeId());
                    for (UUID userId : userIds) {
                        var built = factory.fromPayrollPeriodApproved(event, slip, userId);
                        service.create(built.notification(), built.idempotencyKey(), built.emailRequest());
                    }
                }
                page++;
            } while (slips != null && !slips.isLast());
            log.info("consumed PayrollPeriodApprovedEvent periodId={}", event.getPeriodId());
        } catch (Exception e) {
            log.error("Failed to process PayrollPeriodApprovedEvent {}: {}", event.getPeriodId(), e.getMessage(), e);
        }
    }
}

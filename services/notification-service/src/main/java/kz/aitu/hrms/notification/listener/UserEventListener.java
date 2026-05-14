package kz.aitu.hrms.notification.listener;

import kz.aitu.hrms.notification.config.RabbitConfig;
import kz.aitu.hrms.notification.event.dto.PasswordResetRequestedEvent;
import kz.aitu.hrms.notification.event.dto.UserAccountCreatedEvent;
import kz.aitu.hrms.notification.service.NotificationFactory;
import kz.aitu.hrms.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final NotificationFactory factory;
    private final NotificationService service;

    @RabbitListener(queues = RabbitConfig.Q_USER_ACCOUNT_CREATED)
    public void onUserAccountCreated(UserAccountCreatedEvent event) {
        try {
            var built = factory.fromUserAccountCreated(event, event.getUserId());
            service.create(built.notification(), built.idempotencyKey(), built.emailRequest());
            log.info("consumed UserAccountCreatedEvent userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to process UserAccountCreatedEvent {}: {}", event.getUserId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitConfig.Q_PASSWORD_RESET)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        try {
            var built = factory.fromPasswordResetRequested(event, event.getUserId());
            service.create(built.notification(), built.idempotencyKey(), built.emailRequest());
            log.info("consumed PasswordResetRequestedEvent userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to process PasswordResetRequestedEvent {}: {}", event.getUserId(), e.getMessage(), e);
        }
    }
}

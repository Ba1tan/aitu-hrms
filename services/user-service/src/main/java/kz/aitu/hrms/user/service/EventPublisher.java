package kz.aitu.hrms.user.service;

import kz.aitu.hrms.user.config.RabbitConfig;
import kz.aitu.hrms.user.event.PasswordResetRequestedEvent;
import kz.aitu.hrms.user.event.UserAccountCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(UserAccountCreatedEvent event) {
        send(RabbitConfig.RK_USER_ACCOUNT_CREATED, event);
    }

    public void publish(PasswordResetRequestedEvent event) {
        send(RabbitConfig.RK_PASSWORD_RESET, event);
    }

    private void send(String routingKey, Object event) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, routingKey, event);
        } catch (AmqpException ex) {
            log.warn("RabbitMQ unavailable, event {} dropped: {}", routingKey, ex.getMessage());
        }
    }
}
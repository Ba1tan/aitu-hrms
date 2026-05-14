package kz.aitu.hrms.notification.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(SmtpEmailSender.class)
@Slf4j
public class NoopEmailSender implements EmailSender {

    @Override
    public void send(EmailRequest request) {
        log.info("Email NOOP — to={}, template={}", request.to(), request.templateName());
    }
}

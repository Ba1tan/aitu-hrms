package kz.aitu.hrms.notification.service.email;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "notification.mail.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final EmailTemplateRenderer renderer;

    @Value("${notification.mail.from:no-reply@hrms.kz}")
    private String from;

    @Override
    @Async("emailExecutor")
    public void send(EmailRequest request) {
        try {
            String html = renderer.render(request.templateName(), request.variables());
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(request.to());
            helper.setSubject(request.subject());
            helper.setText(html, true);
            mailSender.send(msg);
            log.info("Email sent to={} template={}", request.to(), request.templateName());
        } catch (Exception e) {
            log.error("Email send failed to={} template={}: {}", request.to(), request.templateName(), e.getMessage(), e);
        }
    }
}

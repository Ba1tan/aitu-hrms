package kz.aitu.hrms.notification.service.email;

import java.util.Map;

public record EmailRequest(
        String to,
        String subject,
        String templateName,
        Map<String, Object> variables
) {}

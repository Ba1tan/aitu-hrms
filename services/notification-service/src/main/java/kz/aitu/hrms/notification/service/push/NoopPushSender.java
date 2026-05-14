package kz.aitu.hrms.notification.service.push;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(FcmPushSender.class)
@Slf4j
public class NoopPushSender implements PushSender {

    @Override
    public void send(String deviceToken, String title, String body) {
        log.info("Push NOOP — token={}, title={}", deviceToken, title);
    }
}

package kz.aitu.hrms.notification.service.push;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "notification.push.enabled", havingValue = "true")
public class FcmPushSender implements PushSender {

    @Override
    public void send(String deviceToken, String title, String body) {
        // TODO(v2): implement FCM push. Add com.google.firebase:firebase-admin:9.x dependency.
        throw new UnsupportedOperationException("FCM push not implemented in v1");
    }
}

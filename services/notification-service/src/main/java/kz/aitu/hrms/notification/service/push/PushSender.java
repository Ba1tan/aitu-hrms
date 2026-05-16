package kz.aitu.hrms.notification.service.push;

public interface PushSender {
    void send(String deviceToken, String title, String body);
}

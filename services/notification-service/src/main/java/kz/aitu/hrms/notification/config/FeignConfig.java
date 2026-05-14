package kz.aitu.hrms.notification.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "kz.aitu.hrms.notification.client")
public class FeignConfig {
}

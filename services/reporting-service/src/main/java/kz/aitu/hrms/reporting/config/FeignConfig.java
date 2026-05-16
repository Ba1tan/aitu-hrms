package kz.aitu.hrms.reporting.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "kz.aitu.hrms.reporting.client")
public class FeignConfig {
}

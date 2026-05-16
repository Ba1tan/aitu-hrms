package kz.aitu.hrms.integration.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "kz.aitu.hrms.integration.client")
public class FeignConfig {
}

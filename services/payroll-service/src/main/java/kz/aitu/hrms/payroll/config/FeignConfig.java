package kz.aitu.hrms.payroll.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "kz.aitu.hrms.payroll.client")
public class FeignConfig {
}

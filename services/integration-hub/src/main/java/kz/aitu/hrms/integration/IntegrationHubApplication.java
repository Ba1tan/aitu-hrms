package kz.aitu.hrms.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "kz.aitu.hrms.integration",
        "kz.aitu.hrms.common"
})
public class IntegrationHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(IntegrationHubApplication.class, args);
    }
}

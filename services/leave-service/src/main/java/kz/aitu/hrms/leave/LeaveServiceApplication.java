package kz.aitu.hrms.leave;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "kz.aitu.hrms.leave",
        "kz.aitu.hrms.common"
})
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "kz.aitu.hrms.leave.repository")
@EnableFeignClients(basePackages = "kz.aitu.hrms.leave.client")
public class LeaveServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LeaveServiceApplication.class, args);
    }
}
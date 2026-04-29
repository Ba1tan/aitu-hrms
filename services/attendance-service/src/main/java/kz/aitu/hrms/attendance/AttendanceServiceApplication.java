package kz.aitu.hrms.attendance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
        "kz.aitu.hrms.attendance",
        "kz.aitu.hrms.common"
})
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "kz.aitu.hrms.attendance.repository")
@EnableFeignClients(basePackages = "kz.aitu.hrms.attendance.client")
@EnableAsync
public class AttendanceServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AttendanceServiceApplication.class, args);
    }
}
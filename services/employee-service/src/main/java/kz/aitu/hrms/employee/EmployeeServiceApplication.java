package kz.aitu.hrms.employee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "kz.aitu.hrms.employee",
        "kz.aitu.hrms.common"
})
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "kz.aitu.hrms.employee.repository")
@EnableFeignClients(basePackages = "kz.aitu.hrms.employee.client")
public class EmployeeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmployeeServiceApplication.class, args);
    }
}
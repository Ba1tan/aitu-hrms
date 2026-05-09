package kz.aitu.hrms.payroll;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
        "kz.aitu.hrms.payroll",
        "kz.aitu.hrms.common"
})
@EnableAsync
public class PayrollServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayrollServiceApplication.class, args);
    }
}
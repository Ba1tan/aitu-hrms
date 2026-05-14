package kz.aitu.hrms.notification.client;

import kz.aitu.hrms.notification.client.dto.PayslipBriefDto;
import kz.aitu.hrms.notification.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "payroll-service",
             url = "${payroll-service.url}",
             configuration = FeignConfig.class)
public interface PayrollClient {

    @GetMapping("/v1/payroll/periods/{periodId}/payslips")
    Page<PayslipBriefDto> listPayslips(
            @PathVariable UUID periodId,
            @RequestParam int page,
            @RequestParam int size);
}

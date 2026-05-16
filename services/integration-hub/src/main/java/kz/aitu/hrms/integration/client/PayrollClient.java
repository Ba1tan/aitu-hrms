package kz.aitu.hrms.integration.client;

import kz.aitu.hrms.integration.client.dto.PageResponse;
import kz.aitu.hrms.integration.client.dto.PayrollPeriodDto;
import kz.aitu.hrms.integration.client.dto.PayslipDetailDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "payroll-service", url = "${payroll-service.url}",
        configuration = kz.aitu.hrms.integration.config.FeignConfig.class)
public interface PayrollClient {

    @GetMapping("/api/v1/payroll/periods/{id}")
    PayrollPeriodDto getPeriod(@PathVariable UUID id);

    @GetMapping("/api/v1/payroll/periods/{id}/payslips")
    PageResponse<PayslipDetailDto> listPayslipsForPeriod(
            @PathVariable UUID id,
            @RequestParam int page,
            @RequestParam int size);
}

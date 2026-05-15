package kz.aitu.hrms.reporting.client;

import kz.aitu.hrms.reporting.client.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "payroll-client", url = "${clients.payroll-service}")
public interface PayrollClient {

    @GetMapping("/api/v1/payroll/periods/latest")
    PayrollPeriodDto getLatestPeriod();

    @GetMapping("/api/v1/payroll/periods")
    PageResponse<PayrollPeriodDto> listPeriods(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size);

    @GetMapping("/api/v1/payroll/periods/{id}/totals")
    PayrollTotalsDto getPeriodTotals(@PathVariable UUID id);

    @GetMapping("/api/v1/payroll/periods/{id}/payslips")
    PageResponse<PayslipDto> listPayslips(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size);

    @GetMapping("/api/v1/payroll/my-payslips")
    List<PayslipDto> myPayslips(@RequestParam(defaultValue = "1") int limit);
}

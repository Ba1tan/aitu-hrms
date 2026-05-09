package kz.aitu.hrms.payroll.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * leave-service exposes balances; payroll uses the consolidated attendance
 * summary's {@code onLeaveDays} as the authoritative leave count for the
 * month, but this client is here for future "unpaid leave" specific lookups.
 */
@FeignClient(
        name = "leave-service",
        url = "${app.services.leave-service-uri}",
        path = "/v1"
)
public interface LeaveClient {

    @GetMapping("/leave/balances/employee/{id}")
    EmployeeClient.Envelope<Object> balancesForEmployee(
            @PathVariable("id") UUID employeeId,
            @RequestParam(value = "year", required = false) Integer year);
}
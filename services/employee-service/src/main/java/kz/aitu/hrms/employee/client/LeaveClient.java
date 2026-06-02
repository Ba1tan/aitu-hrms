package kz.aitu.hrms.employee.client;

import kz.aitu.hrms.employee.client.dto.ActiveLeaveDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

/**
 * Read-side calls to leave-service. Currently used to enrich employee
 * responses with an "on leave today?" derived flag — the leave flow never
 * writes to employees.status, so the badge has to be computed at read time.
 */
@FeignClient(name = "leave-client", url = "${app.services.leave-service-uri}")
public interface LeaveClient {

    @GetMapping("/api/v1/leave/active")
    List<ActiveLeaveDto> activeToday(@RequestParam(name = "employeeIds", required = false) List<UUID> employeeIds);
}
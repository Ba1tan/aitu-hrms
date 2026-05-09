package kz.aitu.hrms.payroll.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-side calls to attendance-service. Used to determine the actual number
 * of days worked when generating payslips, so that prorating is based on
 * recorded attendance rather than an assumed full month.
 */
@FeignClient(
        name = "attendance-service",
        url = "${app.services.attendance-service-uri}",
        path = "/v1"
)
public interface AttendanceClient {

    @GetMapping("/attendance/summary/employee/{id}")
    EmployeeClient.Envelope<EmployeeMonthSummary> summary(
            @PathVariable("id") UUID employeeId,
            @RequestParam("year") int year,
            @RequestParam("month") int month);

    record EmployeeMonthSummary(
            UUID employeeId,
            int year,
            int month,
            long presentDays,
            long lateDays,
            long absentDays,
            long halfDays,
            long onLeaveDays,
            long holidayDays,
            BigDecimal totalWorkedHours,
            BigDecimal overtimeHours
    ) {}
}
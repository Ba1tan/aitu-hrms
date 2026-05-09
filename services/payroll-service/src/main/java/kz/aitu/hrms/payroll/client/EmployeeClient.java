package kz.aitu.hrms.payroll.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Read-side calls to employee-service. Payroll generation depends on:
 *   - {@link #listActive(String, int)} — drive the "for each employee" loop
 *   - {@link #get(UUID)} — pull baseSalary, IIN, residency, pensioner, disability
 */
@FeignClient(
        name = "employee-service",
        url = "${app.services.employee-service-uri}",
        path = "/api/v1"
)
public interface EmployeeClient {

    @GetMapping("/employees/{id}")
    Envelope<EmployeeDetail> get(@PathVariable("id") UUID id);

    @GetMapping("/employees")
    Envelope<PageResponse<EmployeeSummary>> listActive(
            @RequestParam(value = "status", defaultValue = "ACTIVE") String status,
            @RequestParam(value = "size", defaultValue = "1000") int size);

    record EmployeeSummary(
            UUID id,
            String employeeNumber,
            String fullName,
            String email,
            String department,
            String position,
            String status
    ) {}

    record EmployeeDetail(
            UUID id,
            String employeeNumber,
            String firstName,
            String lastName,
            String middleName,
            String fullName,
            String email,
            String iin,
            BigDecimal baseSalary,
            String disabilityGroup,
            Boolean isResident,
            Boolean isPensioner,
            DepartmentSummary department,
            PositionSummary position
    ) {}

    record DepartmentSummary(UUID id, String name) {}
    record PositionSummary(UUID id, String title) {}

    record PageResponse<T>(List<T> content, int totalElements) {}
    record Envelope<T>(boolean success, String message, T data) {}
}
package kz.aitu.hrms.attendance.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

/**
 * Read-side calls to employee-service. Used to resolve names for response
 * decoration and to expand a department into its employee list for the
 * department views and bulk-absent operation.
 *
 * The shared {@link kz.aitu.hrms.common.dto.ApiResponse} envelope is wrapped
 * in {@link Envelope} here to keep this client decoupled from the response
 * generic in hrms-common (Feign needs concrete types for decoding).
 */
@FeignClient(
        name = "employee-service",
        url = "${app.services.employee-service-uri}",
        path = "/api/v1"
)
public interface EmployeeClient {

    @GetMapping("/employees/{id}")
    Envelope<EmployeeSummary> get(@PathVariable("id") UUID id);

    @GetMapping("/employees")
    Envelope<PageResponse<EmployeeSummary>> listByDepartment(
            @RequestParam("departmentId") UUID departmentId,
            @RequestParam(value = "size", defaultValue = "1000") int size);

    @GetMapping("/employees")
    Envelope<PageResponse<EmployeeSummary>> listActive(
            @RequestParam(value = "status", defaultValue = "ACTIVE") String status,
            @RequestParam(value = "size", defaultValue = "1000") int size);

    record EmployeeSummary(
            UUID id,
            String employeeNumber,
            String fullName,
            String firstName,
            String lastName,
            String middleName,
            String email,
            UUID departmentId,
            String department
    ) {}

    record PageResponse<T>(List<T> content, int totalElements) {}

    record Envelope<T>(boolean success, String message, T data) {}
}
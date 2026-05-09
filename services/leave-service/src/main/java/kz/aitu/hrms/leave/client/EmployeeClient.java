package kz.aitu.hrms.leave.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

/**
 * Read-side calls to employee-service. Used to:
 *   - resolve full names for response decoration (calendar, balances)
 *   - look up an employee's manager_id for approval-scope checks
 *   - expand a department into its active employee list
 *   - enumerate active employees (initialize/carryover)
 *
 * The shared {@link kz.aitu.hrms.common.dto.ApiResponse} envelope is wrapped
 * locally so Feign can decode the concrete generic.
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
            String email,
            String department,
            String position,
            String status
    ) {}

    /**
     * Detailed projection used to read manager_id during approval validation.
     * The remote {@code /employees/{id}} endpoint returns more fields — we only
     * decode the ones the leave flow needs.
     */
    record EmployeeDetail(
            UUID id,
            String fullName,
            String email,
            ManagerSummary manager
    ) {}

    record ManagerSummary(UUID id, String fullName) {}

    record PageResponse<T>(List<T> content, int totalElements) {}

    record Envelope<T>(boolean success, String message, T data) {}
}
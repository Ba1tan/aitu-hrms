package kz.aitu.hrms.reporting.client;

import kz.aitu.hrms.reporting.client.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "employee-client", url = "${clients.employee-service}")
public interface EmployeeClient {

    @GetMapping("/api/v1/employees/count")
    EmployeeCountsDto getCounts();

    @GetMapping("/api/v1/employees")
    PageResponse<EmployeeSummaryDto> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size);

    @GetMapping("/api/v1/employees/{id}")
    EmployeeDto getById(@PathVariable UUID id);

    @GetMapping("/api/v1/departments")
    List<DepartmentDto> listDepartments();
}

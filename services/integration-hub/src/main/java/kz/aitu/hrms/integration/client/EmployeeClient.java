package kz.aitu.hrms.integration.client;

import kz.aitu.hrms.integration.client.dto.EmployeeIinDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "employee-service", url = "${employee-service.url}",
        configuration = kz.aitu.hrms.integration.config.FeignConfig.class)
public interface EmployeeClient {

    @GetMapping("/api/v1/employees/{id}")
    EmployeeIinDto getById(@PathVariable UUID id);

    // TODO(employee-service): add bulk endpoint — currently falls back to parallel single calls
    @PostMapping("/api/v1/employees/bulk-by-ids")
    List<EmployeeIinDto> getByIds(@RequestBody List<UUID> ids);
}

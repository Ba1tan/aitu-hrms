package kz.aitu.hrms.notification.client;

import kz.aitu.hrms.notification.client.dto.EmployeeDetailDto;
import kz.aitu.hrms.notification.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "employee-service",
             url = "${employee-service.url}",
             configuration = FeignConfig.class)
public interface EmployeeClient {

    @GetMapping("/v1/employees/{id}")
    EmployeeDetailDto getById(@PathVariable("id") UUID id);
}

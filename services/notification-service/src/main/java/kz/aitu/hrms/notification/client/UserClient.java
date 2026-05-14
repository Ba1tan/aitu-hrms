package kz.aitu.hrms.notification.client;

import kz.aitu.hrms.notification.client.dto.UserBriefDto;
import kz.aitu.hrms.notification.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "user-service",
             url = "${user-service.url}",
             configuration = FeignConfig.class)
public interface UserClient {

    // TODO(user-service): endpoint not yet implemented — see CLAUDE.md §10
    @GetMapping("/v1/users/by-employee/{employeeId}")
    UserBriefDto findByEmployeeId(@PathVariable UUID employeeId);

    // TODO(user-service): endpoint not yet implemented — see CLAUDE.md §10
    @GetMapping("/v1/users/by-permission/{permissionCode}")
    List<UUID> findUserIdsByPermission(@PathVariable String permissionCode);
}

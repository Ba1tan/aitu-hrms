package kz.aitu.hrms.notification.service;

import feign.FeignException;
import io.micrometer.core.instrument.MeterRegistry;
import kz.aitu.hrms.notification.client.EmployeeClient;
import kz.aitu.hrms.notification.client.UserClient;
import kz.aitu.hrms.notification.client.dto.UserBriefDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipientResolver {

    private final UserClient userClient;
    private final EmployeeClient employeeClient;
    private final MeterRegistry metrics;

    public List<UUID> resolveUserIds(UUID employeeId) {
        if (employeeId == null) return List.of();
        try {
            UserBriefDto u = userClient.findByEmployeeId(employeeId);
            return u == null ? List.of() : List.of(u.userId());
        } catch (FeignException.NotFound e) {
            return List.of();
        } catch (Exception e) {
            metrics.counter("notification.recipient.unresolved", "method", "byEmployee").increment();
            log.info("UserClient.findByEmployeeId unavailable: {}", e.getMessage());
            return List.of();
        }
    }

    public List<UUID> resolveUserIdsByPermission(String permission) {
        try {
            return userClient.findUserIdsByPermission(permission);
        } catch (Exception e) {
            metrics.counter("notification.recipient.unresolved", "method", "byPermission").increment();
            log.info("UserClient.findUserIdsByPermission unavailable: {}", e.getMessage());
            return List.of();
        }
    }

    @Cacheable("employee-emails")
    public String resolveEmail(UUID employeeId) {
        try {
            return employeeClient.getById(employeeId).email();
        } catch (Exception e) {
            log.info("Email resolution failed for {}: {}", employeeId, e.getMessage());
            return null;
        }
    }
}

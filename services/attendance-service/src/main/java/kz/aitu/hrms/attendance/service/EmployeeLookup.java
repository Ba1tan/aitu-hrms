package kz.aitu.hrms.attendance.service;

import feign.FeignException;
import kz.aitu.hrms.attendance.client.EmployeeClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Thin wrapper around {@link EmployeeClient} that swallows transport errors
 * and returns {@code null} / empty results so the attendance flow keeps
 * working when employee-service is unreachable. Employee-service only
 * provides response decoration here — it is never on the hot path of a
 * check-in (the AI service alone resolves the employee identity).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeLookup {

    private final EmployeeClient employeeClient;

    public String fullName(UUID employeeId) {
        EmployeeClient.EmployeeSummary s = summary(employeeId);
        return s == null ? null : s.fullName();
    }

    /**
     * Fetches the employee summary (name + departmentId) in one call. Returns
     * {@code null} when employee-service is unreachable so the attendance
     * flow can fall back gracefully (no name decoration, default schedule).
     */
    public EmployeeClient.EmployeeSummary summary(UUID employeeId) {
        if (employeeId == null) return null;
        try {
            EmployeeClient.Envelope<EmployeeClient.EmployeeSummary> resp = employeeClient.get(employeeId);
            if (resp != null && resp.data() != null) {
                return resp.data();
            }
        } catch (FeignException e) {
            log.debug("employee-service lookup failed for {}: {}", employeeId, e.getMessage());
        } catch (Exception e) {
            log.warn("employee-service unexpected error for {}: {}", employeeId, e.getMessage());
        }
        return null;
    }

    public Map<UUID, String> fullNames(List<UUID> employeeIds) {
        Map<UUID, String> out = new HashMap<>();
        for (UUID id : employeeIds) {
            out.put(id, fullName(id));
        }
        return out;
    }

    public List<EmployeeClient.EmployeeSummary> listByDepartment(UUID departmentId) {
        try {
            EmployeeClient.Envelope<EmployeeClient.PageResponse<EmployeeClient.EmployeeSummary>> resp =
                    employeeClient.listByDepartment(departmentId, 1000);
            if (resp != null && resp.data() != null && resp.data().content() != null) {
                return resp.data().content();
            }
        } catch (FeignException e) {
            log.debug("employee-service department list failed for {}: {}", departmentId, e.getMessage());
        } catch (Exception e) {
            log.warn("employee-service unexpected error for dept {}: {}", departmentId, e.getMessage());
        }
        return Collections.emptyList();
    }

    public List<EmployeeClient.EmployeeSummary> listActive() {
        try {
            EmployeeClient.Envelope<EmployeeClient.PageResponse<EmployeeClient.EmployeeSummary>> resp =
                    employeeClient.listActive("ACTIVE", 1000);
            if (resp != null && resp.data() != null && resp.data().content() != null) {
                return resp.data().content();
            }
        } catch (FeignException e) {
            log.debug("employee-service listActive failed: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("employee-service unexpected error in listActive: {}", e.getMessage());
        }
        return Collections.emptyList();
    }
}
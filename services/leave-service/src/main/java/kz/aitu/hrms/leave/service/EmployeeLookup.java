package kz.aitu.hrms.leave.service;

import feign.FeignException;
import kz.aitu.hrms.leave.client.EmployeeClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Wrapper around {@link EmployeeClient} that swallows transport errors and
 * returns {@code null}/empty results so the leave flow remains usable when
 * employee-service is unreachable for read-side calls (display names, etc.).
 *
 * Calls that are load-bearing for business decisions (manager lookup for the
 * approval check) intentionally do NOT swallow — see {@link #managerOrThrow}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeLookup {

    private final EmployeeClient employeeClient;

    public String fullName(UUID employeeId) {
        if (employeeId == null) return null;
        try {
            EmployeeClient.Envelope<EmployeeClient.EmployeeDetail> resp = employeeClient.get(employeeId);
            if (resp != null && resp.data() != null) {
                return resp.data().fullName();
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

    /**
     * Returns the employee's manager id, or {@code null} when employee-service
     * cannot be reached or the employee has none. Caller decides what to do
     * with a {@code null} (the approval check denies LEAVE_APPROVE_TEAM if no
     * manager linkage exists).
     */
    public UUID managerId(UUID employeeId) {
        if (employeeId == null) return null;
        try {
            EmployeeClient.Envelope<EmployeeClient.EmployeeDetail> resp = employeeClient.get(employeeId);
            if (resp != null && resp.data() != null && resp.data().manager() != null) {
                return resp.data().manager().id();
            }
        } catch (FeignException e) {
            log.debug("employee-service manager lookup failed for {}: {}", employeeId, e.getMessage());
        } catch (Exception e) {
            log.warn("employee-service unexpected error in managerId({}): {}", employeeId, e.getMessage());
        }
        return null;
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
package kz.aitu.hrms.payroll.service;

import feign.FeignException;
import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.types.DisabilityGroup;
import kz.aitu.hrms.payroll.calculator.EmployeePayrollProfile;
import kz.aitu.hrms.payroll.client.EmployeeClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Wrapper around {@link EmployeeClient} that:
 *   - swallows transport errors for non-critical reads (logging context)
 *   - throws on critical reads (employee not found during payroll generation)
 *
 * Resolves the {@link EmployeePayrollProfile} payload the calculator needs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeLookup {

    private final EmployeeClient client;

    public EmployeePayrollProfile profileOrThrow(UUID employeeId) {
        EmployeeClient.EmployeeDetail d = detail(employeeId)
                .orElseThrow(() -> new BusinessException(
                        "Employee not found in employee-service: " + employeeId));
        return toProfile(d);
    }

    public Optional<EmployeePayrollProfile> profile(UUID employeeId) {
        return detail(employeeId).map(this::toProfile);
    }

    public List<EmployeeClient.EmployeeSummary> listActive() {
        try {
            EmployeeClient.Envelope<EmployeeClient.PageResponse<EmployeeClient.EmployeeSummary>> resp =
                    client.listActive("ACTIVE", 1000);
            if (resp != null && resp.data() != null && resp.data().content() != null) {
                return resp.data().content();
            }
        } catch (FeignException e) {
            log.warn("employee-service listActive failed: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("employee-service unexpected error in listActive: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private Optional<EmployeeClient.EmployeeDetail> detail(UUID employeeId) {
        if (employeeId == null) return Optional.empty();
        try {
            EmployeeClient.Envelope<EmployeeClient.EmployeeDetail> resp = client.get(employeeId);
            if (resp != null && resp.data() != null) {
                return Optional.of(resp.data());
            }
        } catch (FeignException e) {
            log.warn("employee-service get({}) failed: {}", employeeId, e.getMessage());
        } catch (Exception e) {
            log.warn("employee-service unexpected error for {}: {}", employeeId, e.getMessage());
        }
        return Optional.empty();
    }

    private EmployeePayrollProfile toProfile(EmployeeClient.EmployeeDetail d) {
        DisabilityGroup group = parseDisabilityGroup(d.disabilityGroup());
        boolean disabled = group != null && group != DisabilityGroup.NONE;
        return EmployeePayrollProfile.builder()
                .employeeId(d.id())
                .employeeNumber(d.employeeNumber())
                .iin(d.iin())
                .fullName(d.fullName())
                .departmentName(d.department() != null ? d.department().name() : null)
                .positionTitle(d.position() != null ? d.position().title() : null)
                .baseSalary(d.baseSalary())
                .resident(d.isResident() == null || d.isResident())
                .pensioner(d.isPensioner() != null && d.isPensioner())
                .disabled(disabled)
                .disabilityGroup(group == null ? DisabilityGroup.NONE : group)
                .build();
    }

    private DisabilityGroup parseDisabilityGroup(String raw) {
        if (raw == null || raw.isBlank()) return DisabilityGroup.NONE;
        return switch (raw.trim().toUpperCase()) {
            case "GROUP_1", "GROUP1", "1" -> DisabilityGroup.GROUP_1;
            case "GROUP_2", "GROUP2", "2" -> DisabilityGroup.GROUP_2;
            case "GROUP_3", "GROUP3", "3" -> DisabilityGroup.GROUP_3;
            default -> DisabilityGroup.NONE;
        };
    }
}
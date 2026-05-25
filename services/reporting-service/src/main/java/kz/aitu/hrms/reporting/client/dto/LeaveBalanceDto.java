package kz.aitu.hrms.reporting.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mirrors leave-service {@code LeaveBalanceDtos.Response}: {@code leaveType} is
 * a nested object and the entitlement is {@code entitledDays}. Day counts are
 * integers upstream but kept as {@code BigDecimal} here (Jackson widens them);
 * {@link #getLeaveTypeName()} flattens the type name for report writers.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeaveBalanceDto {
    private UUID employeeId;
    private String employeeName;
    private LeaveTypeInfo leaveType;
    private int year;
    private BigDecimal entitledDays;
    private BigDecimal usedDays;
    private BigDecimal remainingDays;

    public String getLeaveTypeName() {
        return leaveType != null ? leaveType.getName() : null;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LeaveTypeInfo {
        private UUID id;
        private String name;
    }
}

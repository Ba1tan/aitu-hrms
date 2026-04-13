package kz.aitu.hrms.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeCreatedEvent {
    private UUID employeeId;
    private String fullName;
    private String email;
    private BigDecimal salary;
    private UUID departmentId;
}

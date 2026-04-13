package kz.aitu.hrms.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeTerminatedEvent {
    private UUID employeeId;
    private LocalDate terminationDate;
    private String reason;
}

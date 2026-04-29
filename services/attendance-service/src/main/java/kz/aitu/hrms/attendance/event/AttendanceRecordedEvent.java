package kz.aitu.hrms.attendance.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Published whenever an attendance row is created or completed (check-out).
 * Consumed by payroll (worked-hours feed), reporting, and notification services.
 *
 * Routing key: {@code attendance.recorded}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecordedEvent {
    private UUID recordId;
    private UUID employeeId;
    private LocalDate workDate;
    private String status;
    private String method;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private BigDecimal workedHours;
}
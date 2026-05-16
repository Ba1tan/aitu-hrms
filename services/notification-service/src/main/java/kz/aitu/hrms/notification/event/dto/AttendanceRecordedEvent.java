package kz.aitu.hrms.notification.event.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TODO(hrms-common): move to kz.aitu.hrms.common.event when the producer
 * service (attendance-service) is ready to share it. Field shapes must match
 * docs/EVENTS.md §3 exactly — Jackson deserializes by name.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AttendanceRecordedEvent {
    private UUID recordId;
    private UUID employeeId;
    private LocalDate workDate;
    private String status;          // PRESENT|LATE|ABSENT|HALF_DAY|ON_LEAVE|HOLIDAY|WEEKEND
    private String method;          // FACE|MANUAL|WEB|MOBILE
    private LocalDateTime checkIn;  // nullable
    private LocalDateTime checkOut; // nullable
    private BigDecimal workedHours; // nullable
}

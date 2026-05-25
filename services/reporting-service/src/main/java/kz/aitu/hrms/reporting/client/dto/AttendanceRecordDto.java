package kz.aitu.hrms.reporting.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mirrors attendance-service {@code AttendanceDtos.RecordResponse}: a single
 * {@code employeeName}, {@code workDate}, and {@code LocalDateTime} check
 * in/out timestamps.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttendanceRecordDto {
    private UUID id;
    private UUID employeeId;
    private String employeeName;
    private LocalDate workDate;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private String status;
}

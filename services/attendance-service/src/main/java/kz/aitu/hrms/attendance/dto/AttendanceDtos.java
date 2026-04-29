package kz.aitu.hrms.attendance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kz.aitu.hrms.attendance.entity.AttendanceStatus;
import kz.aitu.hrms.attendance.entity.CheckInMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class AttendanceDtos {

    @Data
    public static class ManualCheckInRequest {
        private UUID employeeId;
        private CheckInMethod method;
        private BigDecimal locationLat;
        private BigDecimal locationLng;
    }

    @Data
    public static class ManualCheckOutRequest {
        private UUID employeeId;
        private CheckInMethod method;
        private BigDecimal locationLat;
        private BigDecimal locationLng;
    }

    @Data
    public static class ManualEntryRequest {
        @NotNull
        private UUID employeeId;
        @NotNull
        private LocalDate workDate;
        private LocalDateTime checkIn;
        private LocalDateTime checkOut;
        @NotNull
        private AttendanceStatus status;
        @Size(max = 1000)
        private String notes;
    }

    @Data
    public static class UpdateRecordRequest {
        private LocalDateTime checkIn;
        private LocalDateTime checkOut;
        private AttendanceStatus status;
        @Size(max = 1000)
        private String notes;
    }

    @Data
    public static class BulkAbsentRequest {
        @NotNull
        private LocalDate date;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckInResponse {
        private UUID id;
        private UUID employeeId;
        private String employeeName;
        private LocalDate workDate;
        private LocalDateTime checkIn;
        private LocalDateTime checkOut;
        private AttendanceStatus status;
        private CheckInMethod method;
        private BigDecimal workedHours;
        private Double faceConfidence;
        private BigDecimal fraudScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodayResponse {
        private boolean checkedIn;
        private boolean checkedOut;
        private LocalDateTime checkInTime;
        private LocalDateTime checkOutTime;
        private AttendanceStatus status;
        private CheckInMethod method;
        private BigDecimal workedHours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordResponse {
        private UUID id;
        private UUID employeeId;
        private String employeeName;
        private LocalDate workDate;
        private LocalDateTime checkIn;
        private LocalDateTime checkOut;
        private AttendanceStatus status;
        private CheckInMethod checkInMethod;
        private CheckInMethod checkOutMethod;
        private BigDecimal workedHours;
        private Integer overtimeMinutes;
        private BigDecimal fraudScore;
        private String fraudFlags;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkAbsentResponse {
        private LocalDate date;
        private int marked;
        private int skipped;
    }
}
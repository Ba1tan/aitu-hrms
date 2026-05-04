package kz.aitu.hrms.attendance.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

public class ScheduleDtos {

    @Data
    public static class CreateScheduleRequest {
        @NotBlank @Size(max = 100)
        private String name;
        @NotNull
        private LocalTime workStartTime;
        @NotNull
        private LocalTime workEndTime;
        @Min(0)
        private Integer lateThresholdMin;
        @Min(0)
        private Integer halfDayThresholdMin;
        private UUID departmentId;
        private Boolean isDefault;
    }

    @Data
    public static class UpdateScheduleRequest {
        @Size(max = 100)
        private String name;
        private LocalTime workStartTime;
        private LocalTime workEndTime;
        @Min(0)
        private Integer lateThresholdMin;
        @Min(0)
        private Integer halfDayThresholdMin;
        private UUID departmentId;
        private Boolean isDefault;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleResponse {
        private UUID id;
        private String name;
        private LocalTime workStartTime;
        private LocalTime workEndTime;
        private Integer lateThresholdMin;
        private Integer halfDayThresholdMin;
        private UUID departmentId;
        private boolean isDefault;
    }
}
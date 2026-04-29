package kz.aitu.hrms.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

public class HolidayDtos {

    @Data
    public static class CreateHolidayRequest {
        @NotBlank @Size(max = 200)
        private String name;
        @NotNull
        private LocalDate holidayDate;
        private Boolean annual;
        @Size(max = 1000)
        private String description;
    }

    @Data
    public static class UpdateHolidayRequest {
        @Size(max = 200)
        private String name;
        private LocalDate holidayDate;
        private Boolean annual;
        @Size(max = 1000)
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HolidayResponse {
        private UUID id;
        private String name;
        private LocalDate holidayDate;
        private boolean annual;
        private String description;
    }
}
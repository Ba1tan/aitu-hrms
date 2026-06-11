package kz.aitu.hrms.attendance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    // JSON contract uses `date` / `isAnnual` (what the frontend sends and reads);
    // the Java fields keep their descriptive entity names.
    @Data
    public static class CreateHolidayRequest {
        @NotBlank @Size(max = 200)
        private String name;
        @NotNull
        @JsonProperty("date")
        private LocalDate holidayDate;
        @JsonProperty("isAnnual")
        private Boolean annual;
        @Size(max = 1000)
        private String description;
    }

    @Data
    public static class UpdateHolidayRequest {
        @Size(max = 200)
        private String name;
        @JsonProperty("date")
        private LocalDate holidayDate;
        @JsonProperty("isAnnual")
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
        @JsonProperty("date")
        private LocalDate holidayDate;
        @JsonProperty("isAnnual")
        private boolean annual;
        private String description;
    }
}
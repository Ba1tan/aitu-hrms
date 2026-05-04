package kz.aitu.hrms.leave.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

public class LeaveTypeDtos {

    @Data
    public static class UpsertRequest {
        @NotBlank @Size(max = 100)
        private String name;

        @Size(max = 40)
        private String code;

        @Min(0)
        private int daysAllowed;

        private Boolean isPaid;
        private Boolean requiresApproval;
        private Boolean carryoverAllowed;

        @Min(0)
        private Integer carryoverMaxDays;

        @Size(max = 2000)
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private String name;
        private String code;
        private int daysAllowed;
        private boolean isPaid;
        private boolean requiresApproval;
        private boolean carryoverAllowed;
        private int carryoverMaxDays;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private UUID id;
        private String name;
        private boolean isPaid;
    }
}
package kz.aitu.hrms.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class BiometricDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BiometricStatusResponse {
        private boolean enrolled;
        private String method;
        private LocalDateTime enrolledAt;
        private List<String> photoUrls;
    }
}
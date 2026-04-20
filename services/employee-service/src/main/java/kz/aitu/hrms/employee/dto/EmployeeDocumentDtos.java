package kz.aitu.hrms.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class EmployeeDocumentDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentResponse {
        private UUID id;
        private String documentType;
        private String fileName;
        private String contentType;
        private Long fileSize;
        private LocalDate expiryDate;
        private LocalDateTime uploadedAt;
    }
}
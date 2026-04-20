package kz.aitu.hrms.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

public class EmergencyContactDtos {

    @Data
    public static class CreateContactRequest {
        @NotBlank @Size(max = 200)
        private String name;
        @Size(max = 50)
        private String relationship;
        @NotBlank @Size(max = 20)
        private String phone;
        @Email @Size(max = 255)
        private String email;
        private Boolean primary;
    }

    @Data
    public static class UpdateContactRequest {
        @Size(max = 200) private String name;
        @Size(max = 50)  private String relationship;
        @Size(max = 20)  private String phone;
        @Email @Size(max = 255) private String email;
        private Boolean primary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactResponse {
        private UUID id;
        private String name;
        private String relationship;
        private String phone;
        private String email;
        private boolean primary;
    }
}
package kz.aitu.hrms.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserDtos {

    private UserDtos() {}

    @Data
    public static class CreateUserRequest {
        @Email @NotBlank
        private String email;

        @NotBlank @Size(min = 8)
        private String password;

        @NotBlank
        private String firstName;

        @NotBlank
        private String lastName;

        @Size(max = 20)
        private String phone;

        @NotBlank
        private String role;

        private UUID employeeId;

        private Boolean requirePasswordChange;
    }

    @Data
    public static class UpdateUserRequest {
        private String firstName;
        private String lastName;
        @Size(max = 20)
        private String phone;
        private String role;
        private Boolean enabled;
        private Boolean locked;
    }

    @Data
    public static class LinkEmployeeRequest {
        @NotNull
        private UUID employeeId;
    }

    @Data
    public static class UserSummary {
        private UUID id;
        private String email;
        private String firstName;
        private String lastName;
        private String phone;
        private String role;
        private boolean enabled;
        private boolean locked;
        private UUID employeeId;
        private LocalDateTime lastLoginAt;
        private LocalDateTime createdAt;
    }
}
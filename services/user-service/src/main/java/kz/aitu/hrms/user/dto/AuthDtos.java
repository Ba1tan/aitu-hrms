package kz.aitu.hrms.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AuthDtos {

    private AuthDtos() {}

    @Data
    public static class LoginRequest {
        @Email @NotBlank
        private String email;
        @NotBlank
        private String password;
    }

    @Data
    public static class RefreshTokenRequest {
        @NotBlank
        private String refreshToken;
    }

    @Data
    public static class ForgotPasswordRequest {
        @Email @NotBlank
        private String email;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank
        private String token;
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
        private String newPassword;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
        private String newPassword;
    }

    @Data
    public static class UpdateOwnProfileRequest {
        @Size(max = 100)
        private String firstName;
        @Size(max = 100)
        private String lastName;
        @Size(max = 20)
        private String phone;
    }

    @Data
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private long expiresInMs;
        private UserInfo user;

        @Data
        public static class UserInfo {
            private UUID id;
            private String email;
            private String firstName;
            private String lastName;
            private String role;
            private UUID employeeId;
            private boolean requirePasswordChange;
        }
    }

    @Data
    public static class ProfileResponse {
        private UUID id;
        private String email;
        private String firstName;
        private String lastName;
        private String phone;
        private String role;
        private UUID employeeId;
        private boolean requirePasswordChange;
        private LocalDateTime lastLoginAt;
        private List<String> permissions;
    }
}
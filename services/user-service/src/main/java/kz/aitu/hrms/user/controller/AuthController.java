package kz.aitu.hrms.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.user.dto.AuthDtos;
import kz.aitu.hrms.user.entity.User;
import kz.aitu.hrms.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Login, password reset, profile, tokens")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login with email + password")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDtos.AuthResponse>> login(
            @Valid @RequestBody AuthDtos.LoginRequest request,
            HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request, http)));
    }

    @Operation(summary = "Refresh access token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthDtos.AuthResponse>> refresh(
            @Valid @RequestBody AuthDtos.RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request)));
    }

    @Operation(summary = "Request a password-reset link")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgot(
            @Valid @RequestBody AuthDtos.ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.noContent("If that email exists, a reset link was sent"));
    }

    @Operation(summary = "Reset password using the emailed token")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> reset(
            @Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.noContent("Password has been reset"));
    }

    @Operation(summary = "Invalidate the current token")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(ApiResponse.noContent("Logged out"));
    }

    @Operation(summary = "Change own password")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal User current,
            @Valid @RequestBody AuthDtos.ChangePasswordRequest request) {
        authService.changePassword(current, request);
        return ResponseEntity.ok(ApiResponse.noContent("Password changed"));
    }

    @Operation(summary = "Get current user profile")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthDtos.ProfileResponse>> me(
            @AuthenticationPrincipal User current) {
        return ResponseEntity.ok(ApiResponse.ok(authService.getProfile(current)));
    }

    @Operation(summary = "Update own profile (name, phone)")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<AuthDtos.ProfileResponse>> updateMe(
            @AuthenticationPrincipal User current,
            @Valid @RequestBody AuthDtos.UpdateOwnProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.updateProfile(current, request)));
    }
}
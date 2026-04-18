package kz.aitu.hrms.user.service;

import jakarta.servlet.http.HttpServletRequest;
import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.user.dto.AuthDtos;
import kz.aitu.hrms.user.entity.User;
import kz.aitu.hrms.user.event.PasswordResetRequestedEvent;
import kz.aitu.hrms.user.repository.RolePermissionRepository;
import kz.aitu.hrms.user.repository.UserRepository;
import kz.aitu.hrms.user.security.JwtService;
import kz.aitu.hrms.user.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_LOGIN = 5;

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService blacklist;
    private final PasswordResetService passwordResetService;
    private final EventPublisher eventPublisher;
    private final AuditService auditService;

    @Value("${app.security.lockout-minutes:30}")
    private int lockoutMinutes;

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request, HttpServletRequest http) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Invalid email or password"));

        if (user.isDeleted()) {
            throw new BusinessException("Invalid email or password");
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Account is locked. Try again after "
                    + user.getLockedUntil());
        }
        if (!user.isEnabled()) {
            throw new BusinessException("Account is disabled. Contact your administrator.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordFailedLogin(user);
            throw new BusinessException("Invalid email or password");
        }

        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setAccountNonLocked(true);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(clientIp(http));
        userRepository.save(user);

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthDtos.AuthResponse refresh(AuthDtos.RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (blacklist.isBlacklisted(token) || !jwtService.isValid(token) || !jwtService.isRefreshToken(token)) {
            throw new BusinessException("Invalid or expired refresh token");
        }

        java.util.UUID userId = java.util.UUID.fromString(jwtService.extractUserId(token));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (!user.isEnabled() || user.isDeleted()) {
            throw new BusinessException("Account is no longer active");
        }

        blacklist.blacklist(token, jwtService.remainingLifeMs(token));
        return buildAuthResponse(user);
    }

    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException("Authorization header missing");
        }
        String token = authHeader.substring(7);
        blacklist.blacklist(token, jwtService.remainingLifeMs(token));
        log.info("User logged out");
    }

    @Transactional
    public void changePassword(User current, AuthDtos.ChangePasswordRequest request) {
        User user = userRepository.findById(current.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", current.getId()));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setRequirePasswordChange(false);
        userRepository.save(user);

        auditService.log(user.getId(), user.getEmail(), "CHANGE_PASSWORD",
                "USER", user.getId(), null, null);
        log.info("Password changed: {}", user.getEmail());
    }

    @Transactional
    public void forgotPassword(AuthDtos.ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail())
                .filter(u -> !u.isDeleted() && u.isEnabled())
                .ifPresent(user -> {
                    String token = passwordResetService.issueToken(user.getId());
                    eventPublisher.publish(PasswordResetRequestedEvent.builder()
                            .userId(user.getId())
                            .email(user.getEmail())
                            .firstName(user.getFirstName())
                            .resetToken(token)
                            .ttlSeconds(passwordResetService.ttlSeconds())
                            .build());
                });
        // Response is the same whether the email exists or not.
    }

    @Transactional
    public void resetPassword(AuthDtos.ResetPasswordRequest request) {
        java.util.UUID userId = passwordResetService.consume(request.getToken())
                .orElseThrow(() -> new BusinessException("Invalid or expired reset token"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Invalid or expired reset token"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setAccountNonLocked(true);
        user.setRequirePasswordChange(false);
        userRepository.save(user);

        auditService.log(user.getId(), user.getEmail(), "RESET_PASSWORD",
                "USER", user.getId(), null, null);
    }

    @Transactional(readOnly = true)
    public AuthDtos.ProfileResponse getProfile(User current) {
        User user = userRepository.findById(current.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", current.getId()));

        AuthDtos.ProfileResponse profile = new AuthDtos.ProfileResponse();
        profile.setId(user.getId());
        profile.setEmail(user.getEmail());
        profile.setFirstName(user.getFirstName());
        profile.setLastName(user.getLastName());
        profile.setPhone(user.getPhone());
        profile.setRole(user.getRole().name());
        profile.setEmployeeId(user.getEmployeeId());
        profile.setRequirePasswordChange(user.isRequirePasswordChange());
        profile.setLastLoginAt(user.getLastLoginAt());
        profile.setPermissions(new ArrayList<>(rolePermissionRepository.findPermissionCodesByRole(user.getRole())));
        return profile;
    }

    @Transactional
    public AuthDtos.ProfileResponse updateProfile(User current, AuthDtos.UpdateOwnProfileRequest request) {
        User user = userRepository.findById(current.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", current.getId()));
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null)  user.setLastName(request.getLastName());
        if (request.getPhone() != null)     user.setPhone(request.getPhone());
        userRepository.save(user);
        return getProfile(user);
    }

    // ── helpers ──

    private void recordFailedLogin(User user) {
        int attempts = user.getFailedLoginCount() + 1;
        user.setFailedLoginCount(attempts);
        if (attempts >= MAX_FAILED_LOGIN) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutMinutes));
            log.warn("Account locked after {} failed attempts: {}", attempts, user.getEmail());
        }
        userRepository.save(user);
    }

    private AuthDtos.AuthResponse buildAuthResponse(User user) {
        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        AuthDtos.AuthResponse.UserInfo info = new AuthDtos.AuthResponse.UserInfo();
        info.setId(user.getId());
        info.setEmail(user.getEmail());
        info.setFirstName(user.getFirstName());
        info.setLastName(user.getLastName());
        info.setRole(user.getRole().name());
        info.setEmployeeId(user.getEmployeeId());
        info.setRequirePasswordChange(user.isRequirePasswordChange());

        AuthDtos.AuthResponse resp = new AuthDtos.AuthResponse();
        resp.setAccessToken(accessToken);
        resp.setRefreshToken(refreshToken);
        resp.setExpiresInMs(jwtService.remainingLifeMs(accessToken));
        resp.setUser(info);
        return resp;
    }

    private String clientIp(HttpServletRequest req) {
        if (req == null) return null;
        String fwd = req.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    @SuppressWarnings("unused")
    private List<String> noPermissions() {
        return List.of();
    }
}
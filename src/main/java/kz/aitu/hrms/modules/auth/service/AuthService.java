package kz.aitu.hrms.modules.auth.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.common.security.JwtService;
import kz.aitu.hrms.modules.auth.dto.AuthDtos;
import kz.aitu.hrms.modules.auth.entity.Role;
import kz.aitu.hrms.modules.auth.entity.User;
import kz.aitu.hrms.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final Duration BLACKLIST_TTL = Duration.ofDays(7);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final StringRedisTemplate redisTemplate;


    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new BusinessException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if (!user.isEnabled()) {
            throw new BusinessException("Account is disabled. Contact your administrator.");
        }
        if (!user.isAccountNonLocked()) {
            throw new BusinessException("Account is locked. Contact your administrator.");
        }

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered: " + request.getEmail());
        }

        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid role: " + request.getRole()
                    + ". Valid roles: SUPER_ADMIN, HR_MANAGER, ACCOUNTANT, EMPLOYEE");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .enabled(true)
                .accountNonLocked(true)
                .build();

        userRepository.save(user);
        log.info("New user registered: {} with role {}", user.getEmail(), role);
        return buildAuthResponse(user);
    }

    public AuthDtos.AuthResponse refreshToken(AuthDtos.RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (isTokenBlacklisted(refreshToken)) {
            throw new BusinessException("Refresh token has been invalidated. Please log in again.");
        }

        String email;
        try {
            email = jwtService.extractUsername(refreshToken);
        } catch (Exception ex) {
            throw new BusinessException("Invalid or expired refresh token.");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new BusinessException("Invalid or expired refresh token.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        blacklistToken(refreshToken);
        log.debug("Token refreshed for user: {}", email);
        return buildAuthResponse(user);
    }

    @Transactional
    public void changePassword(AuthDtos.ChangePasswordRequest request) {
        // Get currently authenticated user from security context
        String email = getCurrentUserEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", email);
    }

    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException("Invalid authorization header.");
        }
        String token = authHeader.substring(7);
        blacklistToken(token);
        log.info("Token blacklisted on logout");
    }

    // Private helpers

    private AuthDtos.AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        AuthDtos.AuthResponse.UserInfo userInfo = new AuthDtos.AuthResponse.UserInfo();
        userInfo.setId(user.getId().toString());
        userInfo.setEmail(user.getEmail());
        userInfo.setFirstName(user.getFirstName());
        userInfo.setLastName(user.getLastName());
        userInfo.setRole(user.getRole().name());

        AuthDtos.AuthResponse response = new AuthDtos.AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setUser(userInfo);
        return response;
    }

    private void blacklistToken(String token) {
        try {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + token,
                    "blacklisted",
                    BLACKLIST_TTL
            );
        } catch (Exception ex) {
            log.warn("Redis unavailable, token blacklisting skipped: {}", ex.getMessage());
        }
    }

    private boolean isTokenBlacklisted(String token) {
        try {
            return redisTemplate.hasKey(BLACKLIST_PREFIX + token);
        } catch (Exception ex) {
            log.warn("Redis unavailable, skipping blacklist check: {}", ex.getMessage());
            return false;
        }
    }

    private String getCurrentUserEmail() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException("No authenticated user found.");
        }
        return auth.getName();
    }
}
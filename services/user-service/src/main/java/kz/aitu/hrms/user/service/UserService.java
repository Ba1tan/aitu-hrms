package kz.aitu.hrms.user.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.user.dto.UserDtos;
import kz.aitu.hrms.user.entity.Role;
import kz.aitu.hrms.user.entity.User;
import kz.aitu.hrms.user.event.UserAccountCreatedEvent;
import kz.aitu.hrms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final EventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<UserDtos.UserSummary> list(String search, String role, Pageable pageable) {
        Role roleFilter = parseRoleOrNull(role);
        String query = (search == null || search.isBlank()) ? null : search.trim();
        return userRepository.search(query, roleFilter, pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public UserDtos.UserSummary get(UUID id) {
        return toSummary(find(id));
    }

    @Transactional
    public UserDtos.UserSummary create(UserDtos.CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered: " + request.getEmail());
        }
        Role role = parseRole(request.getRole());

        boolean mustChange = Boolean.TRUE.equals(request.getRequirePasswordChange());

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .phone(request.getPhone())
                .employeeId(request.getEmployeeId())
                .enabled(true)
                .accountNonLocked(true)
                .requirePasswordChange(mustChange)
                .failedLoginCount(0)
                .build();
        user = userRepository.save(user);

        UUID actorId = currentUserIdOrNull();
        String actorEmail = currentUserEmailOrNull();

        auditService.log(actorId, actorEmail, "CREATE", "USER", user.getId(), null, toSummary(user));
        eventPublisher.publish(UserAccountCreatedEvent.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .employeeId(user.getEmployeeId())
                .temporaryPassword(mustChange ? request.getPassword() : null)
                .createdAt(LocalDateTime.now())
                .build());
        log.info("User created: {} ({})", user.getEmail(), user.getRole());
        return toSummary(user);
    }

    @Transactional
    public UserDtos.UserSummary update(UUID id, UserDtos.UpdateUserRequest request) {
        User user = find(id);
        UserDtos.UserSummary before = toSummary(user);

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null)  user.setLastName(request.getLastName());
        if (request.getPhone() != null)     user.setPhone(request.getPhone());
        if (request.getRole() != null)      user.setRole(parseRole(request.getRole()));
        if (request.getEnabled() != null)   user.setEnabled(request.getEnabled());
        if (request.getLocked() != null) {
            user.setAccountNonLocked(!request.getLocked());
            if (Boolean.FALSE.equals(request.getLocked())) {
                user.setLockedUntil(null);
                user.setFailedLoginCount(0);
            }
        }
        userRepository.save(user);

        auditService.log(currentUserIdOrNull(), currentUserEmailOrNull(),
                "UPDATE", "USER", user.getId(), before, toSummary(user));
        return toSummary(user);
    }

    @Transactional
    public void softDelete(UUID id) {
        User user = find(id);
        user.setDeleted(true);
        user.setEnabled(false);
        userRepository.save(user);
        auditService.log(currentUserIdOrNull(), currentUserEmailOrNull(),
                "DELETE", "USER", user.getId(), null, null);
        log.info("User soft-deleted: {}", user.getEmail());
    }

    @Transactional
    public UserDtos.UserSummary linkEmployee(UUID userId, UUID employeeId) {
        Optional<User> existing = userRepository.findByEmployeeId(employeeId);
        if (existing.isPresent() && !existing.get().getId().equals(userId)) {
            throw new BusinessException("Employee already linked to another user: " + existing.get().getEmail());
        }

        User user = find(userId);
        user.setEmployeeId(employeeId);
        userRepository.save(user);

        auditService.log(currentUserIdOrNull(), currentUserEmailOrNull(),
                "LINK_EMPLOYEE", "USER", user.getId(), null, employeeId);
        return toSummary(user);
    }

    // ── internals ──

    private User find(UUID id) {
        return userRepository.findById(id)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    private Role parseRole(String role) {
        try {
            return Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid role: " + role);
        }
    }

    private Role parseRoleOrNull(String role) {
        if (role == null || role.isBlank()) return null;
        return parseRole(role);
    }

    private UserDtos.UserSummary toSummary(User user) {
        UserDtos.UserSummary s = new UserDtos.UserSummary();
        s.setId(user.getId());
        s.setEmail(user.getEmail());
        s.setFirstName(user.getFirstName());
        s.setLastName(user.getLastName());
        s.setPhone(user.getPhone());
        s.setRole(user.getRole().name());
        s.setEnabled(user.isEnabled());
        s.setLocked(!user.isAccountNonLocked()
                || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())));
        s.setEmployeeId(user.getEmployeeId());
        s.setLastLoginAt(user.getLastLoginAt());
        s.setCreatedAt(user.getCreatedAt());
        return s;
    }

    private UUID currentUserIdOrNull() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (principal instanceof User u) ? u.getId() : null;
    }

    private String currentUserEmailOrNull() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (principal instanceof User u) ? u.getEmail() : null;
    }
}
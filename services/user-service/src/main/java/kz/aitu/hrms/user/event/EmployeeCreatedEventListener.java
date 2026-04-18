package kz.aitu.hrms.user.event;

import kz.aitu.hrms.common.event.EmployeeCreatedEvent;
import kz.aitu.hrms.user.config.RabbitConfig;
import kz.aitu.hrms.user.dto.UserDtos;
import kz.aitu.hrms.user.repository.UserRepository;
import kz.aitu.hrms.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * When the employee-service creates an employee with an email, we auto-provision
 * an EMPLOYEE user account. The account starts with require_password_change=true
 * and a random password; the user is expected to go through forgot-password.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeCreatedEventListener {

    private static final SecureRandom RNG = new SecureRandom();

    private final UserService userService;
    private final UserRepository userRepository;

    @RabbitListener(queues = RabbitConfig.Q_EMPLOYEE_CREATED)
    public void onEmployeeCreated(EmployeeCreatedEvent event) {
        if (event == null || event.getEmail() == null || event.getEmail().isBlank()) {
            log.debug("EmployeeCreatedEvent without email; skipping user creation");
            return;
        }
        if (userRepository.existsByEmail(event.getEmail())) {
            log.debug("User already exists for {}; skipping", event.getEmail());
            return;
        }

        String[] parts = splitName(event.getFullName());

        UserDtos.CreateUserRequest req = new UserDtos.CreateUserRequest();
        req.setEmail(event.getEmail());
        req.setFirstName(parts[0]);
        req.setLastName(parts[1]);
        req.setPassword(generateTempPassword());
        req.setRole("EMPLOYEE");
        req.setEmployeeId(event.getEmployeeId());
        req.setRequirePasswordChange(true);

        try {
            userService.create(req);
            log.info("Auto-provisioned EMPLOYEE account for {}", event.getEmail());
        } catch (Exception ex) {
            log.error("Failed to auto-provision user for employee {}: {}",
                    event.getEmployeeId(), ex.getMessage());
        }
    }

    private String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) return new String[]{"Employee", "-"};
        String[] tokens = fullName.trim().split("\\s+", 2);
        return tokens.length == 1 ? new String[]{tokens[0], "-"} : tokens;
    }

    private String generateTempPassword() {
        byte[] b = new byte[18];
        RNG.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
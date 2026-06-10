package kz.aitu.hrms.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import kz.aitu.hrms.user.dto.AdminDtos;
import kz.aitu.hrms.user.entity.AuditLog;
import kz.aitu.hrms.user.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Read side of the audit log for {@code GET /v1/users/audit}. The data is
 * written by {@link AuditService} from every sensitive write path; this just
 * exposes a filtered, paginated view for the admin UI.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<AdminDtos.AuditLogResponse> search(String actor, String entityType, String action,
                                                   LocalDate from, LocalDate to, Pageable pageable) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (actor != null && !actor.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("userEmail")),
                        "%" + actor.trim().toLowerCase() + "%"));
            }
            if (entityType != null && !entityType.isBlank()) {
                predicates.add(cb.equal(root.get("entityType"), entityType.trim()));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(root.get("action"), action.trim()));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            }
            if (to != null) {
                // inclusive of the whole "to" day
                predicates.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return auditLogRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private AdminDtos.AuditLogResponse toResponse(AuditLog log) {
        return AdminDtos.AuditLogResponse.builder()
                .id(log.getId())
                .timestamp(log.getCreatedAt())
                .actorId(log.getUserId())
                .actorEmail(log.getUserEmail())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .ipAddress(log.getIpAddress())
                .oldValue(parse(log.getOldValue()))
                .newValue(parse(log.getNewValue()))
                .build();
    }

    /** Turn the stored JSONB string back into a JSON node so it serializes as a nested object. */
    private JsonNode parse(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            log.warn("Audit value not parseable as JSON, dropping: {}", ex.getMessage());
            return null;
        }
    }
}
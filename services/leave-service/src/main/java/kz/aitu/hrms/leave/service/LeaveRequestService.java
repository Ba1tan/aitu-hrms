package kz.aitu.hrms.leave.service;

import kz.aitu.hrms.common.event.LeaveApprovedEvent;
import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.leave.client.EmployeeClient;
import kz.aitu.hrms.leave.dto.LeaveRequestDtos;
import kz.aitu.hrms.leave.entity.LeaveBalance;
import kz.aitu.hrms.leave.entity.LeaveRequest;
import kz.aitu.hrms.leave.entity.LeaveStatus;
import kz.aitu.hrms.leave.entity.LeaveType;
import kz.aitu.hrms.leave.event.LeaveRejectedEvent;
import kz.aitu.hrms.leave.event.LeaveRequestCreatedEvent;
import kz.aitu.hrms.leave.repository.LeaveBalanceRepository;
import kz.aitu.hrms.leave.repository.LeaveRequestRepository;
import kz.aitu.hrms.leave.repository.LeaveTypeRepository;
import kz.aitu.hrms.leave.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private static final String AUTH_APPROVE_TEAM = "LEAVE_APPROVE_TEAM";
    private static final String AUTH_APPROVE_ALL  = "LEAVE_APPROVE_ALL";

    private final LeaveRequestRepository requestRepo;
    private final LeaveTypeRepository typeRepo;
    private final LeaveBalanceRepository balanceRepo;
    private final EmployeeLookup employeeLookup;
    private final EventPublisher events;
    private final LeaveMapper mapper;

    @Value("${app.zone:Asia/Almaty}")
    private String zoneId;

    private LocalDate today() {
        return LocalDate.now(ZoneId.of(zoneId));
    }

    @Transactional
    public LeaveRequestDtos.Response create(LeaveRequestDtos.CreateRequest req) {
        UUID employeeId = CurrentUser.employeeId();
        if (employeeId == null) {
            throw new BusinessException("Caller has no associated employee profile");
        }
        if (req.getStartDate() == null || req.getEndDate() == null) {
            throw new BusinessException("startDate and endDate are required");
        }
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new BusinessException("endDate cannot be before startDate");
        }
        if (req.getStartDate().isBefore(today())) {
            throw new BusinessException("startDate cannot be in the past");
        }

        LeaveType type = typeRepo.findByIdAndDeletedFalse(req.getLeaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("LeaveType", req.getLeaveTypeId()));

        int days = (int) ChronoUnit.DAYS.between(req.getStartDate(), req.getEndDate()) + 1;

        // Overlap with PENDING/APPROVED requests for same employee.
        List<LeaveRequest> overlapping = requestRepo.findOverlapping(
                employeeId, req.getStartDate(), req.getEndDate());
        if (!overlapping.isEmpty()) {
            throw new BusinessException(
                    "Overlapping leave request exists for these dates");
        }

        // Balance check. If the balance row is missing (employee seeded via
        // SQL bypassing EmployeeCreatedEvent, or new leave type added after
        // the employee was created, or RMQ message lost), auto-create it
        // with the type's default entitlement so the employee isn't blocked.
        // The unique index on (employee_id, leave_type_id, year) keeps this
        // safe under concurrent requests — duplicate inserts fail and the
        // retry finds the existing row.
        int year = req.getStartDate().getYear();
        LeaveBalance balance = balanceRepo.findOne(employeeId, type.getId(), year)
                .orElseGet(() -> {
                    log.info("Auto-initializing missing balance for employee={} type={} year={}",
                            employeeId, type.getName(), year);
                    return balanceRepo.save(LeaveBalance.builder()
                            .employeeId(employeeId)
                            .leaveType(type)
                            .year(year)
                            .entitledDays(type.getDaysAllowed())
                            .build());
                });
        if (balance.getRemainingDays() < days) {
            throw new BusinessException("Insufficient balance: requested " + days
                    + " day(s), remaining " + balance.getRemainingDays());
        }

        LeaveRequest request = LeaveRequest.builder()
                .employeeId(employeeId)
                .leaveType(type)
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .daysRequested(days)
                .reason(req.getReason())
                .status(LeaveStatus.PENDING)
                .build();
        request = requestRepo.save(request);

        events.publishRequestCreated(LeaveRequestCreatedEvent.builder()
                .requestId(request.getId())
                .employeeId(employeeId)
                .managerId(employeeLookup.managerId(employeeId))
                .leaveType(type.getName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .daysRequested(days)
                .build());

        return mapper.toRequest(request, employeeLookup.fullName(employeeId),
                approverRef(employeeId));
    }

    @Transactional(readOnly = true)
    public Page<LeaveRequestDtos.Response> own(LeaveStatus status, Pageable pageable) {
        UUID employeeId = CurrentUser.employeeId();
        if (employeeId == null) {
            throw new BusinessException("Caller has no associated employee profile");
        }
        String name = employeeLookup.fullName(employeeId);
        // Own requests all belong to the caller, so the approver (their manager)
        // is the same for every row — resolve it once.
        LeaveRequestDtos.EmployeeRef approver = approverRef(employeeId);
        return requestRepo.findOwn(employeeId, status, pageable)
                .map(r -> mapper.toRequest(r, name, approver));
    }

    @Transactional(readOnly = true)
    public LeaveRequestDtos.Response detail(UUID id) {
        LeaveRequest req = requireRequest(id);
        UUID caller = CurrentUser.employeeId();
        boolean isOwner = caller != null && caller.equals(req.getEmployeeId());
        boolean canApproveAll = CurrentUser.hasAuthority(AUTH_APPROVE_ALL);
        boolean canApproveTeam = CurrentUser.hasAuthority(AUTH_APPROVE_TEAM)
                && caller != null && caller.equals(employeeLookup.managerId(req.getEmployeeId()));
        if (!isOwner && !canApproveAll && !canApproveTeam) {
            throw new AccessDeniedException("Not allowed to view this leave request");
        }
        return mapper.toRequest(req, employeeLookup.fullName(req.getEmployeeId()),
                approverRef(req.getEmployeeId()));
    }

    @Transactional
    public LeaveRequestDtos.Response approve(UUID id, LeaveRequestDtos.ReviewRequest reviewReq) {
        LeaveRequest req = requireRequest(id);
        if (req.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessException("Only PENDING requests can be approved (current: " + req.getStatus() + ")");
        }
        validateApprovalAuthority(req.getEmployeeId());

        // Atomic: status flip + balance.usedDays += days
        LeaveType type = req.getLeaveType();
        int year = req.getStartDate().getYear();
        LeaveBalance balance = balanceRepo.findOne(req.getEmployeeId(), type.getId(), year)
                .orElseThrow(() -> new BusinessException(
                        "No balance found for employee/type/year — cannot approve"));
        if (balance.getRemainingDays() < req.getDaysRequested()) {
            throw new BusinessException(
                    "Insufficient balance at approval time: requested " + req.getDaysRequested()
                            + " day(s), remaining " + balance.getRemainingDays());
        }
        balance.setUsedDays(balance.getUsedDays() + req.getDaysRequested());
        balanceRepo.save(balance);

        req.setStatus(LeaveStatus.APPROVED);
        req.setReviewedBy(CurrentUser.userId());
        req.setReviewedAt(LocalDateTime.now(ZoneId.of(zoneId)));
        if (reviewReq != null && reviewReq.getComment() != null) {
            req.setReviewComment(reviewReq.getComment());
        }
        req = requestRepo.save(req);

        events.publishApproved(LeaveApprovedEvent.builder()
                .requestId(req.getId())
                .employeeId(req.getEmployeeId())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .leaveType(type.getName())
                .build());

        return mapper.toRequest(req, employeeLookup.fullName(req.getEmployeeId()),
                approverRef(req.getEmployeeId()));
    }

    @Transactional
    public LeaveRequestDtos.Response reject(UUID id, LeaveRequestDtos.ReviewRequest reviewReq) {
        LeaveRequest req = requireRequest(id);
        if (req.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessException("Only PENDING requests can be rejected (current: " + req.getStatus() + ")");
        }
        validateApprovalAuthority(req.getEmployeeId());

        req.setStatus(LeaveStatus.REJECTED);
        req.setReviewedBy(CurrentUser.userId());
        req.setReviewedAt(LocalDateTime.now(ZoneId.of(zoneId)));
        if (reviewReq != null) req.setReviewComment(reviewReq.getComment());
        req = requestRepo.save(req);

        events.publishRejected(LeaveRejectedEvent.builder()
                .requestId(req.getId())
                .employeeId(req.getEmployeeId())
                .reviewedBy(CurrentUser.userId())
                .comment(req.getReviewComment())
                .build());

        return mapper.toRequest(req, employeeLookup.fullName(req.getEmployeeId()),
                approverRef(req.getEmployeeId()));
    }

    /**
     * Cancel a request:
     *   - PENDING: the owning employee can cancel; LEAVE_APPROVE_ALL can also cancel.
     *   - APPROVED: only LEAVE_APPROVE_ALL (HR) can cancel; balance is reversed in the
     *               same transaction.
     *   - REJECTED/CANCELLED: not allowed.
     */
    @Transactional
    public LeaveRequestDtos.Response cancel(UUID id) {
        LeaveRequest req = requireRequest(id);
        UUID caller = CurrentUser.employeeId();
        boolean canApproveAll = CurrentUser.hasAuthority(AUTH_APPROVE_ALL);

        switch (req.getStatus()) {
            case PENDING -> {
                boolean isOwner = caller != null && caller.equals(req.getEmployeeId());
                if (!isOwner && !canApproveAll) {
                    throw new AccessDeniedException("Only the owner or HR can cancel a pending request");
                }
            }
            case APPROVED -> {
                if (!canApproveAll) {
                    throw new AccessDeniedException("Only HR can cancel an approved request");
                }
                LeaveType type = req.getLeaveType();
                int year = req.getStartDate().getYear();
                int days = req.getDaysRequested();
                balanceRepo.findOne(req.getEmployeeId(), type.getId(), year).ifPresent(b -> {
                    b.setUsedDays(Math.max(0, b.getUsedDays() - days));
                    balanceRepo.save(b);
                });
            }
            default -> throw new BusinessException(
                    "Cannot cancel a request in status " + req.getStatus());
        }

        req.setStatus(LeaveStatus.CANCELLED);
        req.setReviewedBy(CurrentUser.userId());
        req.setReviewedAt(LocalDateTime.now(ZoneId.of(zoneId)));
        req = requestRepo.save(req);
        return mapper.toRequest(req, employeeLookup.fullName(req.getEmployeeId()),
                approverRef(req.getEmployeeId()));
    }

    @Transactional(readOnly = true)
    public Page<LeaveRequestDtos.Response> pendingForApprover(Pageable pageable) {
        UUID caller = CurrentUser.employeeId();
        if (CurrentUser.hasAuthority(AUTH_APPROVE_ALL)) {
            return requestRepo.findAll(LeaveStatus.PENDING, pageable)
                    .map(this::decorate);
        }
        if (CurrentUser.hasAuthority(AUTH_APPROVE_TEAM)) {
            if (caller == null) {
                throw new BusinessException("Caller has no associated employee profile");
            }
            List<UUID> reportIds = directReportIds(caller);
            if (reportIds.isEmpty()) {
                return Page.empty(pageable);
            }
            return requestRepo.findPendingForApprovers(reportIds, LeaveStatus.PENDING, pageable)
                    .map(this::decorate);
        }
        throw new AccessDeniedException("Approval permission required");
    }

    @Transactional(readOnly = true)
    public Page<LeaveRequestDtos.Response> team(LeaveStatus status, Pageable pageable) {
        UUID caller = CurrentUser.employeeId();
        if (caller == null) {
            throw new BusinessException("Caller has no associated employee profile");
        }
        List<UUID> reportIds = directReportIds(caller);
        if (reportIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return requestRepo.findByEmployees(reportIds, status, pageable)
                .map(this::decorate);
    }

    @Transactional(readOnly = true)
    public Page<LeaveRequestDtos.Response> all(LeaveStatus status, UUID departmentId, Pageable pageable) {
        if (departmentId == null) {
            return requestRepo.findAll(status, pageable).map(this::decorate);
        }
        List<EmployeeClient.EmployeeSummary> employees = employeeLookup.listByDepartment(departmentId);
        if (employees.isEmpty()) return Page.empty(pageable);
        List<UUID> ids = employees.stream().map(EmployeeClient.EmployeeSummary::id).toList();
        return requestRepo.findByEmployees(ids, status, pageable).map(this::decorate);
    }

    private LeaveRequestDtos.Response decorate(LeaveRequest r) {
        return mapper.toRequest(r, employeeLookup.fullName(r.getEmployeeId()));
    }

    /**
     * Resolves the manager who will review this employee's request, as an
     * {@link LeaveRequestDtos.EmployeeRef}, or {@code null} when no manager is
     * assigned (or employee-service is unreachable). Surfaced to the requester
     * so they can see who their request is waiting on.
     */
    private LeaveRequestDtos.EmployeeRef approverRef(UUID employeeId) {
        EmployeeClient.ManagerSummary m = employeeLookup.manager(employeeId);
        if (m == null) {
            return null;
        }
        return LeaveRequestDtos.EmployeeRef.builder()
                .id(m.id())
                .fullName(m.fullName())
                .build();
    }

    private LeaveRequest requireRequest(UUID id) {
        return requestRepo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", id));
    }

    private void validateApprovalAuthority(UUID employeeId) {
        UUID caller = CurrentUser.employeeId();
        if (caller != null && caller.equals(employeeId)) {
            throw new AccessDeniedException("Cannot approve your own leave request");
        }
        if (CurrentUser.hasAuthority(AUTH_APPROVE_ALL)) {
            return;
        }
        if (CurrentUser.hasAuthority(AUTH_APPROVE_TEAM)) {
            UUID managerId = employeeLookup.managerId(employeeId);
            if (managerId != null && caller != null && caller.equals(managerId)) {
                return;
            }
            throw new AccessDeniedException("Only the employee's manager can approve this request");
        }
        throw new AccessDeniedException("Approval permission required");
    }

    /**
     * Direct-report ids for the caller. employee-service exposes the list via
     * a manager filter on the employee search endpoint, but our current Feign
     * client only knows {@code departmentId}/{@code status}/{@code search}.
     * We approximate by walking the active list and selecting those whose
     * manager matches — bounded by the company's active headcount and only
     * called on the approval paths (low volume).
     *
     * TODO: add a dedicated {@code GET /employees?managerId=} client method
     * once the contract is finalized; the in-memory filter is a fallback.
     */
    private List<UUID> directReportIds(UUID managerEmployeeId) {
        List<EmployeeClient.EmployeeSummary> active = employeeLookup.listActive();
        if (active.isEmpty()) return List.of();
        Map<UUID, UUID> managerOf = new java.util.HashMap<>();
        for (EmployeeClient.EmployeeSummary e : active) {
            UUID m = employeeLookup.managerId(e.id());
            if (m != null) managerOf.put(e.id(), m);
        }
        return managerOf.entrySet().stream()
                .filter(en -> managerEmployeeId.equals(en.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }
}
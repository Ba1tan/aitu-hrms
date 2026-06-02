package kz.aitu.hrms.leave.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.leave.client.EmployeeClient;
import kz.aitu.hrms.leave.dto.LeaveBalanceDtos;
import kz.aitu.hrms.leave.entity.BalanceAdjustment;
import kz.aitu.hrms.leave.entity.LeaveBalance;
import kz.aitu.hrms.leave.entity.LeaveType;
import kz.aitu.hrms.leave.repository.BalanceAdjustmentRepository;
import kz.aitu.hrms.leave.repository.LeaveBalanceRepository;
import kz.aitu.hrms.leave.repository.LeaveTypeRepository;
import kz.aitu.hrms.leave.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveBalanceService {

    private final LeaveBalanceRepository balanceRepo;
    private final LeaveTypeRepository typeRepo;
    private final BalanceAdjustmentRepository adjustmentRepo;
    private final EmployeeLookup employeeLookup;
    private final LeaveMapper mapper;

    @Value("${app.zone:Asia/Almaty}")
    private String zoneId;

    @Value("${app.leave.carryover-max-percent:0.5}")
    private double carryoverMaxPercent;

    private int currentYear() {
        return LocalDate.now(ZoneId.of(zoneId)).getYear();
    }

    @Transactional
    public List<LeaveBalanceDtos.Response> own(UUID employeeId) {
        if (employeeId == null) {
            throw new BusinessException("Caller has no associated employee profile");
        }
        ensureBalances(employeeId, currentYear());
        return forEmployeeYear(employeeId, currentYear(), null);
    }

    @Transactional
    public List<LeaveBalanceDtos.Response> forEmployee(UUID employeeId, Integer year) {
        if (employeeId == null) {
            throw new BusinessException("employeeId is required");
        }
        int y = year == null ? currentYear() : year;
        ensureBalances(employeeId, y);
        String name = employeeLookup.fullName(employeeId);
        return forEmployeeYear(employeeId, y, name);
    }

    /**
     * Create missing balances for this employee against every active leave
     * type for the given year. Idempotent — exits early if all types already
     * have a row. Called from read paths so the UI always shows every
     * entitlement, even for employees seeded via SQL or for new leave types
     * added after the employee was created.
     */
    private void ensureBalances(UUID employeeId, int year) {
        List<LeaveType> types = typeRepo.findAllByDeletedFalseOrderByName();
        List<LeaveBalance> existing = balanceRepo.findForEmployeeYear(employeeId, year);
        if (existing.size() >= types.size()) return;
        java.util.Set<UUID> have = new java.util.HashSet<>();
        for (LeaveBalance b : existing) have.add(b.getLeaveType().getId());
        for (LeaveType type : types) {
            if (have.contains(type.getId())) continue;
            balanceRepo.save(LeaveBalance.builder()
                    .employeeId(employeeId)
                    .leaveType(type)
                    .year(year)
                    .entitledDays(type.getDaysAllowed())
                    .build());
        }
    }

    private List<LeaveBalanceDtos.Response> forEmployeeYear(UUID employeeId, int year, String name) {
        return balanceRepo.findForEmployeeYear(employeeId, year).stream()
                .sorted(Comparator.comparing(b -> b.getLeaveType().getName(),
                        Comparator.nullsLast(String::compareTo)))
                .map(b -> mapper.toBalance(b, name))
                .toList();
    }

    @Transactional(readOnly = true)
    public LeaveBalanceDtos.DepartmentSummary forDepartment(UUID departmentId, Integer year) {
        if (departmentId == null) {
            throw new BusinessException("departmentId is required");
        }
        int y = year == null ? currentYear() : year;
        List<EmployeeClient.EmployeeSummary> employees = employeeLookup.listByDepartment(departmentId);
        if (employees.isEmpty()) {
            return LeaveBalanceDtos.DepartmentSummary.builder()
                    .departmentId(departmentId)
                    .year(y)
                    .balances(List.of())
                    .build();
        }
        List<UUID> ids = employees.stream().map(EmployeeClient.EmployeeSummary::id).toList();
        Map<UUID, String> names = new HashMap<>();
        employees.forEach(e -> names.put(e.id(), e.fullName()));

        List<LeaveBalanceDtos.Response> rows = balanceRepo.findForEmployeesYear(ids, y).stream()
                .sorted(Comparator.comparing((LeaveBalance b) -> names.get(b.getEmployeeId()),
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(b -> b.getLeaveType().getName()))
                .map(b -> mapper.toBalance(b, names.get(b.getEmployeeId())))
                .toList();

        return LeaveBalanceDtos.DepartmentSummary.builder()
                .departmentId(departmentId)
                .year(y)
                .balances(rows)
                .build();
    }

    @Transactional
    public LeaveBalanceDtos.InitializeResponse initialize(int year) {
        List<LeaveType> types = typeRepo.findAllByDeletedFalseOrderByName();
        if (types.isEmpty()) {
            throw new BusinessException("No leave types configured — cannot initialize");
        }
        List<EmployeeClient.EmployeeSummary> active = employeeLookup.listActive();
        if (active.isEmpty()) {
            throw new BusinessException(
                    "Cannot enumerate active employees — employee-service unavailable. Retry later.");
        }
        int created = 0;
        int skipped = 0;
        for (EmployeeClient.EmployeeSummary emp : active) {
            for (LeaveType type : types) {
                if (balanceRepo.findOne(emp.id(), type.getId(), year).isPresent()) {
                    skipped++;
                    continue;
                }
                balanceRepo.save(LeaveBalance.builder()
                        .employeeId(emp.id())
                        .leaveType(type)
                        .year(year)
                        .entitledDays(type.getDaysAllowed())
                        .build());
                created++;
            }
        }
        return LeaveBalanceDtos.InitializeResponse.builder()
                .year(year)
                .employeesProcessed(active.size())
                .balancesCreated(created)
                .balancesSkipped(skipped)
                .build();
    }

    @Transactional
    public LeaveBalanceDtos.Response adjust(UUID balanceId, LeaveBalanceDtos.AdjustRequest req) {
        if (req.getDays() == null || req.getDays() == 0) {
            throw new BusinessException("Adjustment days must be non-zero");
        }
        LeaveBalance balance = balanceRepo.findByIdAndDeletedFalse(balanceId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveBalance", balanceId));

        int newAdjusted = balance.getAdjustedDays() + req.getDays();
        balance.setAdjustedDays(newAdjusted);
        // Floor: an adjustment must never push remaining_days below 0.
        int projectedRemaining = balance.getEntitledDays() + balance.getCarriedOver()
                + newAdjusted - balance.getUsedDays();
        if (projectedRemaining < 0) {
            throw new BusinessException("Adjustment would result in negative balance");
        }
        balance = balanceRepo.saveAndFlush(balance);

        adjustmentRepo.save(BalanceAdjustment.builder()
                .balance(balance)
                .days(req.getDays())
                .reason(req.getReason())
                .adjustedBy(CurrentUser.userId())
                .build());

        // Reload to pick up the fresh remaining_days from the generated column.
        LeaveBalance fresh = balanceRepo.findById(balance.getId()).orElse(balance);
        return mapper.toBalance(fresh, employeeLookup.fullName(fresh.getEmployeeId()));
    }

    /**
     * Roll unused annual leave from {@code fromYear} into {@code fromYear+1} as
     * {@code carriedOver}. Capped per leave type via {@code carryoverMaxDays},
     * and additionally by {@code app.leave.carryover-max-percent} of the
     * employee's entitlement (default 50%). Only types with
     * {@code carryoverAllowed = TRUE} participate.
     */
    @Transactional
    public LeaveBalanceDtos.CarryoverResponse carryover(int fromYear) {
        int toYear = fromYear + 1;
        int processed = 0;
        int totalCarried = 0;

        List<LeaveType> eligible = typeRepo.findAllByDeletedFalseOrderByName().stream()
                .filter(LeaveType::isCarryoverAllowed)
                .toList();
        if (eligible.isEmpty()) {
            return LeaveBalanceDtos.CarryoverResponse.builder()
                    .fromYear(fromYear)
                    .toYear(toYear)
                    .balancesProcessed(0)
                    .totalDaysCarried(0)
                    .build();
        }

        List<UUID> typeIds = eligible.stream().map(LeaveType::getId).toList();
        List<LeaveBalance> all = balanceRepo.findByYearAndTypes(fromYear, typeIds);

        for (LeaveBalance prev : all) {
            LeaveType type = prev.getLeaveType();
            int unused = Math.max(0, prev.getRemainingDays());
            int percentCap = (int) Math.floor(prev.getEntitledDays() * carryoverMaxPercent);
            int typeCap = type.getCarryoverMaxDays() > 0 ? type.getCarryoverMaxDays() : Integer.MAX_VALUE;
            int toCarry = Math.min(unused, Math.min(percentCap, typeCap));
            if (toCarry <= 0) continue;

            LeaveBalance next = balanceRepo.findOne(prev.getEmployeeId(), type.getId(), toYear)
                    .orElseGet(() -> balanceRepo.save(LeaveBalance.builder()
                            .employeeId(prev.getEmployeeId())
                            .leaveType(type)
                            .year(toYear)
                            .entitledDays(type.getDaysAllowed())
                            .build()));
            next.setCarriedOver(next.getCarriedOver() + toCarry);
            balanceRepo.save(next);

            processed++;
            totalCarried += toCarry;
        }

        return LeaveBalanceDtos.CarryoverResponse.builder()
                .fromYear(fromYear)
                .toYear(toYear)
                .balancesProcessed(processed)
                .totalDaysCarried(totalCarried)
                .build();
    }

    /**
     * Hook used by the EmployeeCreatedEvent listener — auto-creates this year's
     * balances for a brand-new employee. Idempotent.
     */
    @Transactional
    public void initializeForEmployee(UUID employeeId) {
        if (employeeId == null) return;
        int year = currentYear();
        List<LeaveType> types = typeRepo.findAllByDeletedFalseOrderByName();
        for (LeaveType type : types) {
            if (balanceRepo.findOne(employeeId, type.getId(), year).isPresent()) continue;
            balanceRepo.save(LeaveBalance.builder()
                    .employeeId(employeeId)
                    .leaveType(type)
                    .year(year)
                    .entitledDays(type.getDaysAllowed())
                    .build());
        }
    }
}
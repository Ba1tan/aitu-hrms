package kz.aitu.hrms.leave.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.leave.client.EmployeeClient;
import kz.aitu.hrms.leave.dto.CalendarDtos;
import kz.aitu.hrms.leave.entity.LeaveRequest;
import kz.aitu.hrms.leave.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaveCalendarService {

    private final LeaveRequestRepository requestRepo;
    private final EmployeeLookup employeeLookup;
    private final LeaveMapper mapper;

    @Value("${app.zone:Asia/Almaty}")
    private String zoneId;

    @Transactional(readOnly = true)
    public List<CalendarDtos.Entry> calendar(Integer year, Integer month, UUID departmentId) {
        LocalDate now = LocalDate.now(ZoneId.of(zoneId));
        int y = year == null ? now.getYear() : year;
        int m = month == null ? now.getMonthValue() : month;
        if (m < 1 || m > 12) {
            throw new BusinessException("month must be 1-12");
        }
        YearMonth ym = YearMonth.of(y, m);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        List<UUID> ids = null;
        Map<UUID, String> names = new HashMap<>();
        if (departmentId != null) {
            List<EmployeeClient.EmployeeSummary> employees = employeeLookup.listByDepartment(departmentId);
            if (employees.isEmpty()) return List.of();
            ids = employees.stream().map(EmployeeClient.EmployeeSummary::id).toList();
            employees.forEach(e -> names.put(e.id(), e.fullName()));
        }

        List<LeaveRequest> approved = requestRepo.findApprovedInRange(ids, from, to);
        if (approved.isEmpty()) return List.of();

        List<UUID> missing = approved.stream()
                .map(LeaveRequest::getEmployeeId)
                .distinct()
                .filter(id -> !names.containsKey(id))
                .toList();
        if (!missing.isEmpty()) {
            names.putAll(employeeLookup.fullNames(missing));
        }

        return approved.stream()
                .sorted(Comparator.comparing(LeaveRequest::getStartDate)
                        .thenComparing(r -> names.get(r.getEmployeeId()),
                                Comparator.nullsLast(String::compareTo)))
                .map(r -> mapper.toCalendarEntry(r, names.get(r.getEmployeeId())))
                .toList();
    }
}
package kz.aitu.hrms.attendance.service;

import kz.aitu.hrms.attendance.client.EmployeeClient;
import kz.aitu.hrms.attendance.dto.SummaryDtos;
import kz.aitu.hrms.attendance.entity.AttendanceStatus;
import kz.aitu.hrms.attendance.repository.AttendanceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SummaryServiceImpl implements SummaryService {

    private final AttendanceRecordRepository recordRepo;
    private final EmployeeLookup employeeLookup;

    @Override
    @Transactional(readOnly = true)
    public SummaryDtos.EmployeeSummary employeeSummary(UUID employeeId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        long workedMin = recordRepo.sumWorkedMinutes(employeeId, from, to);
        long overtimeMin = recordRepo.sumOvertimeMinutes(employeeId, from, to);

        SummaryDtos.EmployeeSummary out = SummaryDtos.EmployeeSummary.builder()
                .employeeId(employeeId)
                .year(year).month(month)
                .presentDays(recordRepo.countByEmployeeAndStatus(employeeId, from, to, AttendanceStatus.PRESENT))
                .lateDays(recordRepo.countByEmployeeAndStatus(employeeId, from, to, AttendanceStatus.LATE))
                .absentDays(recordRepo.countByEmployeeAndStatus(employeeId, from, to, AttendanceStatus.ABSENT))
                .halfDays(recordRepo.countByEmployeeAndStatus(employeeId, from, to, AttendanceStatus.HALF_DAY))
                .onLeaveDays(recordRepo.countByEmployeeAndStatus(employeeId, from, to, AttendanceStatus.ON_LEAVE))
                .holidayDays(recordRepo.countByEmployeeAndStatus(employeeId, from, to, AttendanceStatus.HOLIDAY))
                .totalWorkedHours(toHours(workedMin))
                .overtimeHours(toHours(overtimeMin))
                .build();
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public SummaryDtos.AggregateSummary departmentSummary(UUID departmentId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        List<EmployeeClient.EmployeeSummary> employees = employeeLookup.listByDepartment(departmentId);
        if (employees.isEmpty()) {
            return SummaryDtos.AggregateSummary.builder().year(year).month(month).build();
        }
        List<UUID> ids = employees.stream().map(EmployeeClient.EmployeeSummary::id).toList();
        return aggregate(recordRepo.aggregateStatusByEmployees(ids, from, to), year, month);
    }

    @Override
    @Transactional(readOnly = true)
    public SummaryDtos.AggregateSummary companySummary(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        return aggregate(recordRepo.aggregateStatusCompany(from, to), year, month);
    }

    private SummaryDtos.AggregateSummary aggregate(List<Object[]> rows, int year, int month) {
        Map<AttendanceStatus, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((AttendanceStatus) row[0], (Long) row[1]);
        }
        return SummaryDtos.AggregateSummary.builder()
                .year(year).month(month)
                .presentDays(counts.getOrDefault(AttendanceStatus.PRESENT, 0L))
                .lateDays(counts.getOrDefault(AttendanceStatus.LATE, 0L))
                .absentDays(counts.getOrDefault(AttendanceStatus.ABSENT, 0L))
                .halfDays(counts.getOrDefault(AttendanceStatus.HALF_DAY, 0L))
                .onLeaveDays(counts.getOrDefault(AttendanceStatus.ON_LEAVE, 0L))
                .holidayDays(counts.getOrDefault(AttendanceStatus.HOLIDAY, 0L))
                .build();
    }

    private BigDecimal toHours(long minutes) {
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
}
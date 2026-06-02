package kz.aitu.hrms.attendance.service;

import kz.aitu.hrms.attendance.client.EmployeeClient;
import kz.aitu.hrms.attendance.dto.AttendanceDtos;
import kz.aitu.hrms.attendance.entity.AttendanceRecord;
import kz.aitu.hrms.attendance.entity.AttendanceStatus;
import kz.aitu.hrms.attendance.entity.CheckInMethod;
import kz.aitu.hrms.attendance.entity.Holiday;
import kz.aitu.hrms.attendance.entity.WorkSchedule;
import kz.aitu.hrms.attendance.event.AttendanceRecordedEvent;
import kz.aitu.hrms.attendance.repository.AttendanceRecordRepository;
import kz.aitu.hrms.attendance.repository.HolidayRepository;
import kz.aitu.hrms.attendance.repository.WorkScheduleRepository;
import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRecordRepository recordRepo;
    private final HolidayRepository holidayRepo;
    private final WorkScheduleRepository scheduleRepo;
    private final EmployeeLookup employeeLookup;
    private final EventPublisher events;
    private final AttendanceMapper mapper;

    @Value("${app.zone:Asia/Almaty}")
    private String zoneId;

    private ZoneId zone() {
        return ZoneId.of(zoneId);
    }

    @Override
    @Transactional
    public AttendanceDtos.CheckInResponse checkIn(UUID employeeId,
                                                  CheckInMethod method,
                                                  BigDecimal locationLat,
                                                  BigDecimal locationLng) {
        if (employeeId == null) {
            throw new BusinessException("employeeId is required");
        }
        CheckInMethod resolvedMethod = method == null ? CheckInMethod.WEB : method;
        LocalDate today = LocalDate.now(zone());
        LocalDateTime now = LocalDateTime.now(zone());

        if (recordRepo.existsByEmployeeIdAndWorkDateAndDeletedFalse(employeeId, today)) {
            throw new BusinessException("Already checked in today");
        }

        Optional<Holiday> holiday = holidayRepo.findByHolidayDateAndDeletedFalse(today);
        if (holiday.isPresent()) {
            throw new BusinessException("Today is a holiday: " + holiday.get().getName());
        }

        // Fetch the employee summary once and reuse it for both schedule
        // resolution (department override) and response decoration (name).
        // employee-service unreachable → null → falls back to the
        // company-wide default schedule, same as the legacy behaviour.
        var employee = employeeLookup.summary(employeeId);
        UUID departmentId = employee == null ? null : employee.departmentId();
        WorkSchedule schedule = resolveSchedule(departmentId);

        // "Weekend" is now whichever days the resolved schedule doesn't
        // mark as working. Falls back to Mon–Fri if the column is empty.
        if (!isWorkingDay(today.getDayOfWeek(), schedule)) {
            throw new BusinessException("Today is not a working day under the active schedule");
        }

        AttendanceStatus status = computeStatus(now.toLocalTime(), schedule);

        AttendanceRecord record = AttendanceRecord.builder()
                .employeeId(employeeId)
                .workDate(today)
                .checkIn(now)
                .status(status)
                .checkInMethod(resolvedMethod)
                .locationLat(locationLat)
                .locationLng(locationLng)
                .overtimeMinutes(0)
                .build();
        record = recordRepo.save(record);

        events.publishAttendanceRecorded(AttendanceRecordedEvent.builder()
                .recordId(record.getId())
                .employeeId(employeeId)
                .workDate(today)
                .status(status.name())
                .method(resolvedMethod.name())
                .checkIn(now)
                .build());

        String name = employee == null ? null : employee.fullName();
        return mapper.toCheckIn(record, name);
    }

    @Override
    @Transactional
    public AttendanceDtos.CheckInResponse checkOut(UUID employeeId,
                                                   CheckInMethod method,
                                                   BigDecimal locationLat,
                                                   BigDecimal locationLng) {
        if (employeeId == null) {
            throw new BusinessException("employeeId is required");
        }
        CheckInMethod resolvedMethod = method == null ? CheckInMethod.WEB : method;
        LocalDate today = LocalDate.now(zone());
        LocalDateTime now = LocalDateTime.now(zone());

        AttendanceRecord record = recordRepo
                .findByEmployeeIdAndWorkDateAndDeletedFalse(employeeId, today)
                .orElseThrow(() -> new BusinessException("No check-in found for today"));

        if (record.getCheckOut() != null) {
            throw new BusinessException("Already checked out today");
        }
        if (record.getCheckIn() == null) {
            throw new BusinessException("Cannot check out without a check-in");
        }

        record.setCheckOut(now);
        record.setCheckOutMethod(resolvedMethod);

        // Same department override as check-in. Falls back to the default
        // schedule if employee-service is unreachable.
        var employee = employeeLookup.summary(employeeId);
        UUID departmentId = employee == null ? null : employee.departmentId();
        WorkSchedule schedule = resolveSchedule(departmentId);
        int workedMinutes = (int) Duration.between(record.getCheckIn(), now).toMinutes();
        if (workedMinutes < 0) workedMinutes = 0;
        record.setWorkedMinutes(workedMinutes);

        if ((record.getStatus() == AttendanceStatus.PRESENT || record.getStatus() == AttendanceStatus.LATE)
                && workedMinutes < schedule.getHalfDayThresholdMin()) {
            record.setStatus(AttendanceStatus.HALF_DAY);
        }

        long scheduledEndMinutes = Duration.between(LocalTime.MIDNIGHT, schedule.getWorkEndTime()).toMinutes();
        long actualEndMinutes = Duration.between(LocalTime.MIDNIGHT, now.toLocalTime()).toMinutes();
        int overtime = (int) Math.max(0, actualEndMinutes - scheduledEndMinutes);
        record.setOvertimeMinutes(overtime);

        record = recordRepo.save(record);

        events.publishAttendanceRecorded(AttendanceRecordedEvent.builder()
                .recordId(record.getId())
                .employeeId(employeeId)
                .workDate(today)
                .status(record.getStatus().name())
                .method(resolvedMethod.name())
                .checkIn(record.getCheckIn())
                .checkOut(now)
                .workedHours(mapper.toHours(workedMinutes))
                .build());

        String name = employee == null ? null : employee.fullName();
        return mapper.toCheckIn(record, name);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceDtos.TodayResponse today(UUID employeeId) {
        // Admins / service accounts can hit this endpoint via shared UI (the
        // dashboard's AttendanceWidget). They have no employee record, so
        // return the same shape as "not checked in yet" instead of 400.
        if (employeeId == null) {
            return mapper.toToday(null);
        }
        AttendanceRecord r = recordRepo
                .findByEmployeeIdAndWorkDateAndDeletedFalse(employeeId, LocalDate.now(zone()))
                .orElse(null);
        return mapper.toToday(r);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceDtos.RecordResponse> ownRecords(UUID employeeId, LocalDate from, LocalDate to, Pageable pageable) {
        // Same rationale as today(): admins without an employeeId get an
        // empty page rather than a 400, so shared UI doesn't blow up.
        if (employeeId == null) {
            return Page.empty(pageable);
        }
        return recordRepo.search(employeeId, from, to, pageable)
                .map(r -> mapper.toRecord(r, null));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceDtos.RecordResponse> employeeRecords(UUID employeeId, LocalDate from, LocalDate to, Pageable pageable) {
        if (employeeId == null) {
            throw new BusinessException("employeeId is required");
        }
        String name = employeeLookup.fullName(employeeId);
        return recordRepo.search(employeeId, from, to, pageable)
                .map(r -> mapper.toRecord(r, name));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDtos.RecordResponse> departmentRecords(UUID departmentId, LocalDate date) {
        LocalDate target = date == null ? LocalDate.now(zone()) : date;
        List<EmployeeClient.EmployeeSummary> employees = employeeLookup.listByDepartment(departmentId);
        if (employees.isEmpty()) return List.of();

        List<UUID> ids = employees.stream().map(EmployeeClient.EmployeeSummary::id).toList();
        Map<UUID, String> names = new HashMap<>();
        employees.forEach(e -> names.put(e.id(), e.fullName()));

        List<AttendanceRecord> records = recordRepo.findByEmployeeIdsAndWorkDate(ids, target);
        return records.stream()
                .map(r -> mapper.toRecord(r, names.get(r.getEmployeeId())))
                .sorted(Comparator.comparing((AttendanceDtos.RecordResponse r) -> r.getEmployeeName(),
                        Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDtos.RecordResponse> dailyRecords(LocalDate date) {
        LocalDate target = date == null ? LocalDate.now(zone()) : date;
        List<AttendanceRecord> records = recordRepo.findAllByWorkDateAndDeletedFalse(target);
        if (records.isEmpty()) return List.of();

        List<UUID> ids = records.stream().map(AttendanceRecord::getEmployeeId).distinct().toList();
        Map<UUID, String> names = employeeLookup.fullNames(ids);

        return records.stream()
                .map(r -> mapper.toRecord(r, names.get(r.getEmployeeId())))
                .toList();
    }

    @Override
    @Transactional
    public AttendanceDtos.RecordResponse manualEntry(AttendanceDtos.ManualEntryRequest req) {
        if (recordRepo.existsByEmployeeIdAndWorkDateAndDeletedFalse(req.getEmployeeId(), req.getWorkDate())) {
            throw new BusinessException("Record already exists for employee on " + req.getWorkDate());
        }

        Integer worked = null;
        if (req.getCheckIn() != null && req.getCheckOut() != null) {
            worked = (int) Math.max(0, Duration.between(req.getCheckIn(), req.getCheckOut()).toMinutes());
        }

        AttendanceRecord record = AttendanceRecord.builder()
                .employeeId(req.getEmployeeId())
                .workDate(req.getWorkDate())
                .checkIn(req.getCheckIn())
                .checkOut(req.getCheckOut())
                .status(req.getStatus())
                .checkInMethod(req.getCheckIn() != null ? CheckInMethod.MANUAL : null)
                .checkOutMethod(req.getCheckOut() != null ? CheckInMethod.MANUAL : null)
                .workedMinutes(worked)
                .overtimeMinutes(0)
                .notes(req.getNotes())
                .build();
        record = recordRepo.save(record);

        events.publishAttendanceRecorded(AttendanceRecordedEvent.builder()
                .recordId(record.getId())
                .employeeId(req.getEmployeeId())
                .workDate(req.getWorkDate())
                .status(req.getStatus().name())
                .method("MANUAL")
                .checkIn(req.getCheckIn())
                .checkOut(req.getCheckOut())
                .workedHours(mapper.toHours(worked))
                .build());

        return mapper.toRecord(record, employeeLookup.fullName(req.getEmployeeId()));
    }

    @Override
    @Transactional
    public AttendanceDtos.RecordResponse update(UUID id, AttendanceDtos.UpdateRecordRequest req) {
        AttendanceRecord record = recordRepo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found: " + id));

        if (req.getCheckIn() != null) record.setCheckIn(req.getCheckIn());
        if (req.getCheckOut() != null) record.setCheckOut(req.getCheckOut());
        if (req.getStatus() != null) record.setStatus(req.getStatus());
        if (req.getNotes() != null) record.setNotes(req.getNotes());

        if (record.getCheckIn() != null && record.getCheckOut() != null) {
            int worked = (int) Math.max(0,
                    Duration.between(record.getCheckIn(), record.getCheckOut()).toMinutes());
            record.setWorkedMinutes(worked);
        }

        record = recordRepo.save(record);
        return mapper.toRecord(record, employeeLookup.fullName(record.getEmployeeId()));
    }

    @Override
    @Transactional
    public AttendanceDtos.BulkAbsentResponse bulkAbsent(LocalDate date) {
        LocalDate target = date == null ? LocalDate.now(zone()) : date;

        DayOfWeek dow = target.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            throw new BusinessException("Cannot mark absent on a weekend: " + target);
        }
        if (holidayRepo.existsByHolidayDateAndDeletedFalse(target)) {
            throw new BusinessException("Cannot mark absent on a holiday: " + target);
        }

        List<EmployeeClient.EmployeeSummary> active = employeeLookup.listActive();
        if (active.isEmpty()) {
            throw new BusinessException(
                    "Cannot enumerate active employees — employee-service unavailable. Retry later.");
        }

        int marked = 0;
        int skipped = 0;
        List<AttendanceRecord> toSave = new ArrayList<>();
        for (EmployeeClient.EmployeeSummary emp : active) {
            if (recordRepo.existsByEmployeeIdAndWorkDateAndDeletedFalse(emp.id(), target)) {
                skipped++;
                continue;
            }
            toSave.add(AttendanceRecord.builder()
                    .employeeId(emp.id())
                    .workDate(target)
                    .status(AttendanceStatus.ABSENT)
                    .overtimeMinutes(0)
                    .build());
            marked++;
        }
        if (!toSave.isEmpty()) {
            recordRepo.saveAll(toSave);
        }

        return AttendanceDtos.BulkAbsentResponse.builder()
                .date(target)
                .marked(marked)
                .skipped(skipped)
                .build();
    }

    private WorkSchedule resolveSchedule(UUID departmentId) {
        if (departmentId != null) {
            Optional<WorkSchedule> dept = scheduleRepo.findByDepartmentIdAndDeletedFalse(departmentId);
            if (dept.isPresent()) return dept.get();
        }
        return scheduleRepo.findByIsDefaultTrueAndDeletedFalse().orElseGet(WorkSchedule::fallback);
    }

    private AttendanceStatus computeStatus(LocalTime now, WorkSchedule schedule) {
        LocalTime threshold = schedule.getWorkStartTime().plusMinutes(schedule.getLateThresholdMin());
        return now.isAfter(threshold) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;
    }

    /** Working-day lookup against the schedule's stored CSV (MON,TUE,…). */
    private boolean isWorkingDay(DayOfWeek dow, WorkSchedule schedule) {
        String csv = schedule.getWorkingDays();
        if (csv == null || csv.isBlank()) {
            return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
        }
        String code = switch (dow) {
            case MONDAY    -> "MON";
            case TUESDAY   -> "TUE";
            case WEDNESDAY -> "WED";
            case THURSDAY  -> "THU";
            case FRIDAY    -> "FRI";
            case SATURDAY  -> "SAT";
            case SUNDAY    -> "SUN";
        };
        for (String token : csv.split(",")) {
            if (token.trim().equalsIgnoreCase(code)) return true;
        }
        return false;
    }
}
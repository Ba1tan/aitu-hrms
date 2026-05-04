package kz.aitu.hrms.attendance.service;

import feign.FeignException;
import kz.aitu.hrms.attendance.client.AiMlClient;
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
import kz.aitu.hrms.common.event.FraudAttemptDetectedEvent;
import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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
    private final AiMlClient aiMlClient;
    private final EmployeeLookup employeeLookup;
    private final EventPublisher events;
    private final AttendanceMapper mapper;

    @Value("${app.zone:Asia/Almaty}")
    private String zoneId;

    @Value("${app.attendance.fraud.recent-window-min:30}")
    private int fraudWindowMin;

    @Value("${app.attendance.fraud.block-threshold:0.65}")
    private double fraudBlockThreshold;

    @Value("${app.attendance.fraud.review-threshold:0.30}")
    private double fraudReviewThreshold;

    private ZoneId zone() {
        return ZoneId.of(zoneId);
    }

    // Face check-in / check-out

    @Override
    @Transactional
    public AttendanceDtos.CheckInResponse checkInWithFace(MultipartFile photo) {
        AiMlClient.VerifyResponse verify = verifyFaceOrThrow(photo);
        UUID employeeId = UUID.fromString(verify.employeeId());
        return checkIn(employeeId, CheckInMethod.FACE, null, null, verify.confidence());
    }

    @Override
    @Transactional
    public AttendanceDtos.CheckInResponse checkOutWithFace(MultipartFile photo) {
        AiMlClient.VerifyResponse verify = verifyFaceOrThrow(photo);
        UUID employeeId = UUID.fromString(verify.employeeId());
        return checkOut(employeeId, CheckInMethod.FACE, null, null, verify.confidence());
    }

    private AiMlClient.VerifyResponse verifyFaceOrThrow(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new BusinessException("Photo is required for face check-in");
        }
        AiMlClient.VerifyResponse verify;
        try {
            verify = aiMlClient.verifyFace(photo);
        } catch (FeignException e) {
            log.warn("AI service unavailable for face verification: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Face recognition unavailable. Use manual check-in.");
        } catch (Exception e) {
            log.warn("AI service error during face verification: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Face recognition unavailable. Use manual check-in.");
        }
        if (verify == null || !verify.matched()) {
            String reason = verify == null ? "no_response" : verify.reason();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Face not recognized: " + (reason == null ? "no_match" : reason));
        }
        return verify;
    }

    // Generic check-in / check-out

    @Override
    @Transactional
    public AttendanceDtos.CheckInResponse checkIn(UUID employeeId,
                                                  CheckInMethod method,
                                                  BigDecimal locationLat,
                                                  BigDecimal locationLng,
                                                  Double faceConfidence) {
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

        DayOfWeek dow = today.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            throw new BusinessException("Today is a weekend");
        }

        WorkSchedule schedule = resolveSchedule(null);
        AttendanceStatus status = computeStatus(now.toLocalTime(), schedule);

        // Layer 2 fraud detection (only for FACE/BIOMETRIC, only if a recent record exists).
        BigDecimal fraudScore = null;
        String fraudFlags = null;
        if (resolvedMethod == CheckInMethod.FACE || resolvedMethod == CheckInMethod.BIOMETRIC) {
            Optional<AttendanceRecord> recent = recordRepo
                    .findFirstByEmployeeIdAndDeletedFalseOrderByCreatedAtDesc(employeeId);
            if (recent.isPresent() && isSuspicious(recent.get(), now, locationLat, locationLng)) {
                AiMlClient.FraudResponse fraud = callFraudDetection(recent.get(), employeeId, now,
                        resolvedMethod, locationLat, locationLng);
                if (fraud != null) {
                    fraudScore = BigDecimal.valueOf(fraud.fraudProbability());
                    fraudFlags = fraud.flags() == null ? null : String.join(",", fraud.flags());
                    if (fraud.fraudProbability() >= fraudBlockThreshold) {
                        AttendanceRecord blocked = AttendanceRecord.builder()
                                .employeeId(employeeId)
                                .workDate(today)
                                .checkIn(now)
                                .status(AttendanceStatus.BLOCKED)
                                .checkInMethod(resolvedMethod)
                                .locationLat(locationLat)
                                .locationLng(locationLng)
                                .fraudScore(fraudScore)
                                .fraudFlags(fraudFlags)
                                .build();
                        recordRepo.save(blocked);
                        events.publishFraudDetected(FraudAttemptDetectedEvent.builder()
                                .employeeId(employeeId)
                                .fraudScore(fraud.fraudProbability())
                                .flags(fraudFlags)
                                .deviceId(null)
                                .build());
                        throw new BusinessException(
                                "Check-in blocked: suspicious activity detected. Contact HR.");
                    }
                }
            }
        }

        AttendanceRecord record = AttendanceRecord.builder()
                .employeeId(employeeId)
                .workDate(today)
                .checkIn(now)
                .status(status)
                .checkInMethod(resolvedMethod)
                .locationLat(locationLat)
                .locationLng(locationLng)
                .fraudScore(fraudScore)
                .fraudFlags(fraudFlags)
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

        String name = employeeLookup.fullName(employeeId);
        return mapper.toCheckIn(record, name, faceConfidence);
    }

    @Override
    @Transactional
    public AttendanceDtos.CheckInResponse checkOut(UUID employeeId,
                                                   CheckInMethod method,
                                                   BigDecimal locationLat,
                                                   BigDecimal locationLng,
                                                   Double faceConfidence) {
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

        WorkSchedule schedule = resolveSchedule(null);
        int workedMinutes = (int) Duration.between(record.getCheckIn(), now).toMinutes();
        if (workedMinutes < 0) workedMinutes = 0;
        record.setWorkedMinutes(workedMinutes);

        // Half-day status if worked under threshold and we were marked PRESENT/LATE.
        if ((record.getStatus() == AttendanceStatus.PRESENT || record.getStatus() == AttendanceStatus.LATE)
                && workedMinutes < schedule.getHalfDayThresholdMin()) {
            record.setStatus(AttendanceStatus.HALF_DAY);
        }

        // Overtime — anything past scheduled end time.
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

        String name = employeeLookup.fullName(employeeId);
        return mapper.toCheckIn(record, name, faceConfidence);
    }

    // Today / records / summaries

    @Override
    @Transactional(readOnly = true)
    public AttendanceDtos.TodayResponse today(UUID employeeId) {
        if (employeeId == null) {
            throw new BusinessException("Caller has no associated employee profile");
        }
        AttendanceRecord r = recordRepo
                .findByEmployeeIdAndWorkDateAndDeletedFalse(employeeId, LocalDate.now(zone()))
                .orElse(null);
        return mapper.toToday(r);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceDtos.RecordResponse> ownRecords(UUID employeeId, LocalDate from, LocalDate to, Pageable pageable) {
        if (employeeId == null) {
            throw new BusinessException("Caller has no associated employee profile");
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

    // Helpers

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

    private boolean isSuspicious(AttendanceRecord recent,
                                 LocalDateTime now,
                                 BigDecimal locationLat,
                                 BigDecimal locationLng) {
        if (recent.getCheckIn() == null) return false;
        long minutes = Duration.between(recent.getCheckIn(), now).toMinutes();
        if (minutes >= 0 && minutes < fraudWindowMin) return true;
        // Location jump > ~0.1 degrees (~11 km) within the window is suspicious.
        if (locationLat != null && locationLng != null
                && recent.getLocationLat() != null && recent.getLocationLng() != null) {
            BigDecimal latDiff = locationLat.subtract(recent.getLocationLat()).abs();
            BigDecimal lngDiff = locationLng.subtract(recent.getLocationLng()).abs();
            if (latDiff.compareTo(new BigDecimal("0.1")) > 0
                    || lngDiff.compareTo(new BigDecimal("0.1")) > 0) {
                return true;
            }
        }
        return false;
    }

    private AiMlClient.FraudResponse callFraudDetection(AttendanceRecord recent,
                                                        UUID employeeId,
                                                        LocalDateTime now,
                                                        CheckInMethod method,
                                                        BigDecimal locationLat,
                                                        BigDecimal locationLng) {
        try {
            int minutesSince = (int) Duration.between(recent.getCheckIn(), now).toMinutes();
            return aiMlClient.detectFraud(new AiMlClient.FraudRequest(
                    employeeId.toString(),
                    now,
                    recent.getCheckIn(),
                    minutesSince,
                    method.name(),
                    recent.getCheckInMethod() == null ? null : recent.getCheckInMethod().name(),
                    locationLat == null ? null : locationLat.doubleValue(),
                    locationLng == null ? null : locationLng.doubleValue(),
                    recent.getLocationLat() == null ? null : recent.getLocationLat().doubleValue(),
                    recent.getLocationLng() == null ? null : recent.getLocationLng().doubleValue(),
                    null
            ));
        } catch (Exception e) {
            log.warn("Fraud detection unavailable: {}", e.getMessage());
            return null; // non-critical — let check-in proceed
        }
    }
}
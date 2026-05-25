package kz.aitu.hrms.attendance.service;

import kz.aitu.hrms.attendance.dto.AttendanceDtos;
import kz.aitu.hrms.attendance.entity.CheckInMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AttendanceService {

    AttendanceDtos.CheckInResponse checkIn(UUID employeeId,
                                           CheckInMethod method,
                                           BigDecimal locationLat,
                                           BigDecimal locationLng);

    AttendanceDtos.CheckInResponse checkOut(UUID employeeId,
                                            CheckInMethod method,
                                            BigDecimal locationLat,
                                            BigDecimal locationLng);

    AttendanceDtos.TodayResponse today(UUID employeeId);

    Page<AttendanceDtos.RecordResponse> ownRecords(UUID employeeId, LocalDate from, LocalDate to, Pageable pageable);

    Page<AttendanceDtos.RecordResponse> employeeRecords(UUID employeeId, LocalDate from, LocalDate to, Pageable pageable);

    List<AttendanceDtos.RecordResponse> departmentRecords(UUID departmentId, LocalDate date);

    List<AttendanceDtos.RecordResponse> dailyRecords(LocalDate date);

    AttendanceDtos.RecordResponse manualEntry(AttendanceDtos.ManualEntryRequest req);

    AttendanceDtos.RecordResponse update(UUID id, AttendanceDtos.UpdateRecordRequest req);

    AttendanceDtos.BulkAbsentResponse bulkAbsent(LocalDate date);
}
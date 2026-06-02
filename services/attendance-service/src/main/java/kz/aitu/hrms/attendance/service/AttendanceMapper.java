package kz.aitu.hrms.attendance.service;

import kz.aitu.hrms.attendance.dto.AttendanceDtos;
import kz.aitu.hrms.attendance.dto.HolidayDtos;
import kz.aitu.hrms.attendance.dto.ScheduleDtos;
import kz.aitu.hrms.attendance.entity.AttendanceRecord;
import kz.aitu.hrms.attendance.entity.Holiday;
import kz.aitu.hrms.attendance.entity.WorkSchedule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class AttendanceMapper {

    public AttendanceDtos.RecordResponse toRecord(AttendanceRecord r, String employeeName) {
        return AttendanceDtos.RecordResponse.builder()
                .id(r.getId())
                .employeeId(r.getEmployeeId())
                .employeeName(employeeName)
                .workDate(r.getWorkDate())
                .checkIn(r.getCheckIn())
                .checkOut(r.getCheckOut())
                .status(r.getStatus())
                .checkInMethod(r.getCheckInMethod())
                .checkOutMethod(r.getCheckOutMethod())
                .workedHours(toHours(r.getWorkedMinutes()))
                .overtimeMinutes(r.getOvertimeMinutes())
                .notes(r.getNotes())
                .build();
    }

    public AttendanceDtos.CheckInResponse toCheckIn(AttendanceRecord r, String employeeName) {
        return AttendanceDtos.CheckInResponse.builder()
                .id(r.getId())
                .employeeId(r.getEmployeeId())
                .employeeName(employeeName)
                .workDate(r.getWorkDate())
                .checkIn(r.getCheckIn())
                .checkOut(r.getCheckOut())
                .status(r.getStatus())
                .method(r.getCheckOut() != null ? r.getCheckOutMethod() : r.getCheckInMethod())
                .workedHours(toHours(r.getWorkedMinutes()))
                .build();
    }

    public AttendanceDtos.TodayResponse toToday(AttendanceRecord r) {
        if (r == null) {
            return AttendanceDtos.TodayResponse.builder().checkedIn(false).checkedOut(false).build();
        }
        return AttendanceDtos.TodayResponse.builder()
                .checkedIn(r.getCheckIn() != null)
                .checkedOut(r.getCheckOut() != null)
                .checkInTime(r.getCheckIn())
                .checkOutTime(r.getCheckOut())
                .status(r.getStatus())
                .method(r.getCheckOut() != null ? r.getCheckOutMethod() : r.getCheckInMethod())
                .workedHours(toHours(r.getWorkedMinutes()))
                .build();
    }

    public HolidayDtos.HolidayResponse toHoliday(Holiday h) {
        return HolidayDtos.HolidayResponse.builder()
                .id(h.getId())
                .name(h.getName())
                .holidayDate(h.getHolidayDate())
                .annual(h.isAnnual())
                .description(h.getDescription())
                .build();
    }

    public ScheduleDtos.ScheduleResponse toSchedule(WorkSchedule s) {
        return ScheduleDtos.ScheduleResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .workStartTime(s.getWorkStartTime())
                .workEndTime(s.getWorkEndTime())
                .lateThresholdMin(s.getLateThresholdMin())
                .halfDayThresholdMin(s.getHalfDayThresholdMin())
                .departmentId(s.getDepartmentId())
                .isDefault(s.isDefault())
                .workingDays(parseWorkingDays(s.getWorkingDays()))
                .description(s.getDescription())
                .build();
    }

    /** Splits the persisted CSV back into a list for clients. */
    public static java.util.List<String> parseWorkingDays(String csv) {
        if (csv == null || csv.isBlank()) return java.util.List.of();
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    public BigDecimal toHours(Integer minutes) {
        if (minutes == null) return null;
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
}
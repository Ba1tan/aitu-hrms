package kz.aitu.hrms.reporting.client;

import kz.aitu.hrms.reporting.client.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "attendance-client", url = "${clients.attendance-service}")
public interface AttendanceClient {

    @GetMapping("/api/v1/attendance/summary/company")
    AttendanceTodayCountsDto getTodayCounts(@RequestParam(required = false) String date);

    @GetMapping("/api/v1/attendance/today")
    AttendanceRecordDto myToday();

    // /records/daily returns the full day's records as a plain List (not paginated)
    @GetMapping("/api/v1/attendance/records/daily")
    List<AttendanceRecordDto> daily(@RequestParam String date);

    @GetMapping("/api/v1/attendance/records/employee/{id}")
    PageResponse<AttendanceRecordDto> forEmployee(
            @PathVariable UUID id,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size);
}

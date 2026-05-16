package kz.aitu.hrms.reporting.service.xlsx.reports;

import kz.aitu.hrms.reporting.client.AttendanceClient;
import kz.aitu.hrms.reporting.client.EmployeeClient;
import kz.aitu.hrms.reporting.client.dto.AttendanceRecordDto;
import kz.aitu.hrms.reporting.client.dto.EmployeeSummaryDto;
import kz.aitu.hrms.reporting.client.dto.PageResponse;
import kz.aitu.hrms.reporting.service.xlsx.XlsxWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AttendanceSummaryXlsx {

    private final AttendanceClient attendanceClient;
    private final EmployeeClient employeeClient;

    public void write(int year, int month, OutputStream out) throws IOException {
        try (XlsxWriter w = new XlsxWriter()) {
            w.sheet("Сводка посещаемости");
            w.header("Сотрудник", "Фамилия", "Присутствие", "Отсутствие", "Опоздание");

            List<EmployeeSummaryDto> employees = employeeClient.list(null, "ACTIVE", 0, 500).getContent();
            if (employees == null || employees.isEmpty()) {
                w.writeTo(out);
                return;
            }

            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;

            Map<UUID, long[]> counts = new HashMap<>();
            LocalDate current = start;
            while (!current.isAfter(end)) {
                String dateStr = current.format(fmt);
                int page = 0;
                PageResponse<AttendanceRecordDto> resp;
                do {
                    resp = attendanceClient.daily(dateStr, page++, 200);
                    if (resp == null || resp.getContent() == null) break;
                    for (AttendanceRecordDto r : resp.getContent()) {
                        long[] c = counts.computeIfAbsent(r.getEmployeeId(), k -> new long[3]);
                        if ("PRESENT".equals(r.getStatus())) c[0]++;
                        else if ("ABSENT".equals(r.getStatus())) c[1]++;
                        else if ("LATE".equals(r.getStatus())) c[2]++;
                    }
                } while (!resp.isLast());
                current = current.plusDays(1);
            }

            for (EmployeeSummaryDto e : employees) {
                long[] c = counts.getOrDefault(e.getId(), new long[3]);
                w.row(e.getFirstName(), e.getLastName(), c[0], c[1], c[2]);
            }
            w.writeTo(out);
        }
    }
}

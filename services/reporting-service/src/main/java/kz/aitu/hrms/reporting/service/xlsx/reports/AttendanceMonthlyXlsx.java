package kz.aitu.hrms.reporting.service.xlsx.reports;

import kz.aitu.hrms.reporting.client.AttendanceClient;
import kz.aitu.hrms.reporting.client.dto.AttendanceRecordDto;
import kz.aitu.hrms.reporting.client.dto.PageResponse;
import kz.aitu.hrms.reporting.service.xlsx.XlsxWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class AttendanceMonthlyXlsx {

    private final AttendanceClient attendanceClient;

    public void write(int year, int month, OutputStream out) throws IOException {
        try (XlsxWriter w = new XlsxWriter()) {
            w.sheet("Посещаемость");
            w.header("Дата", "Сотрудник", "Приход", "Уход", "Статус");

            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;

            LocalDate current = start;
            while (!current.isAfter(end)) {
                String dateStr = current.format(fmt);
                int page = 0;
                PageResponse<AttendanceRecordDto> resp;
                do {
                    resp = attendanceClient.daily(dateStr, page++, 200);
                    if (resp == null || resp.getContent() == null) break;
                    for (AttendanceRecordDto r : resp.getContent()) {
                        w.row(r.getWorkDate(), r.getEmployeeName(),
                                r.getCheckIn() != null ? r.getCheckIn().toLocalTime().toString() : null,
                                r.getCheckOut() != null ? r.getCheckOut().toLocalTime().toString() : null,
                                r.getStatus());
                    }
                } while (!resp.isLast());
                current = current.plusDays(1);
            }
            w.writeTo(out);
        }
    }
}

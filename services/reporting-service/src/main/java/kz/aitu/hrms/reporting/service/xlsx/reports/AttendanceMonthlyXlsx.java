package kz.aitu.hrms.reporting.service.xlsx.reports;

import kz.aitu.hrms.reporting.client.AttendanceClient;
import kz.aitu.hrms.reporting.client.dto.AttendanceRecordDto;
import kz.aitu.hrms.reporting.service.xlsx.XlsxWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
                List<AttendanceRecordDto> records =
                        attendanceClient.daily(current.format(fmt));
                if (records != null) {
                    for (AttendanceRecordDto r : records) {
                        w.row(r.getWorkDate(), r.getEmployeeName(),
                                r.getCheckIn() != null ? r.getCheckIn().toLocalTime().toString() : null,
                                r.getCheckOut() != null ? r.getCheckOut().toLocalTime().toString() : null,
                                r.getStatus());
                    }
                }
                current = current.plusDays(1);
            }
            w.writeTo(out);
        }
    }
}

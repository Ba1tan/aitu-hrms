package kz.aitu.hrms.reporting.service.xlsx.reports;

import kz.aitu.hrms.reporting.client.AttendanceClient;
import kz.aitu.hrms.reporting.client.dto.AttendanceRecordDto;
import kz.aitu.hrms.reporting.client.dto.PageResponse;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceMonthlyXlsxTest {

    @Mock AttendanceClient attendanceClient;
    @InjectMocks AttendanceMonthlyXlsx report;

    @TempDir Path tmp;

    @Test
    void writesAttendanceRecordsForMonth() throws Exception {
        AttendanceRecordDto rec = new AttendanceRecordDto();
        rec.setEmployeeFirstName("Айгерим");
        rec.setEmployeeLastName("Бекова");
        rec.setDate(LocalDate.of(2026, 2, 1));
        rec.setCheckIn(LocalTime.of(9, 0));
        rec.setCheckOut(LocalTime.of(18, 0));
        rec.setStatus("PRESENT");

        PageResponse<AttendanceRecordDto> page = new PageResponse<>();
        page.setContent(List.of(rec));
        page.setLast(true);

        PageResponse<AttendanceRecordDto> empty = new PageResponse<>();
        empty.setContent(List.of());
        empty.setLast(true);

        // Feb 2026 has 28 days; stub all daily calls generically
        when(attendanceClient.daily(any(), anyInt(), anyInt())).thenReturn(empty);
        when(attendanceClient.daily(eq("2026-02-01"), eq(0), eq(200))).thenReturn(page);

        File out = tmp.resolve("attendance.xlsx").toFile();
        try (OutputStream os = new FileOutputStream(out)) {
            report.write(2026, 2, os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            var sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Айгерим");
            assertThat(sheet.getRow(1).getCell(5).getStringCellValue()).isEqualTo("PRESENT");
        }
    }

    @Test
    void emptyMonth_onlyHeader() throws Exception {
        PageResponse<AttendanceRecordDto> empty = new PageResponse<>();
        empty.setContent(List.of());
        empty.setLast(true);
        when(attendanceClient.daily(any(), anyInt(), anyInt())).thenReturn(empty);

        File out = tmp.resolve("attendance-empty.xlsx").toFile();
        try (OutputStream os = new FileOutputStream(out)) {
            report.write(2026, 2, os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(0);
        }
    }
}

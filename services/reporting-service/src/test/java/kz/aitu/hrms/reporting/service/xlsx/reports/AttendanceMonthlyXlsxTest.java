package kz.aitu.hrms.reporting.service.xlsx.reports;

import kz.aitu.hrms.reporting.client.AttendanceClient;
import kz.aitu.hrms.reporting.client.dto.AttendanceRecordDto;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceMonthlyXlsxTest {

    @Mock AttendanceClient attendanceClient;
    @InjectMocks AttendanceMonthlyXlsx report;

    @TempDir Path tmp;

    @Test
    void writesAttendanceRecordsForMonth() throws Exception {
        AttendanceRecordDto rec = new AttendanceRecordDto();
        rec.setEmployeeName("Айгерим Бекова");
        rec.setWorkDate(LocalDate.of(2026, 2, 1));
        rec.setCheckIn(LocalDateTime.of(2026, 2, 1, 9, 0));
        rec.setCheckOut(LocalDateTime.of(2026, 2, 1, 18, 0));
        rec.setStatus("PRESENT");

        // Feb 2026 has 28 days; stub all daily calls empty, override day 1.
        when(attendanceClient.daily(any())).thenReturn(List.of());
        when(attendanceClient.daily(eq("2026-02-01"))).thenReturn(List.of(rec));

        File out = tmp.resolve("attendance.xlsx").toFile();
        try (OutputStream os = new FileOutputStream(out)) {
            report.write(2026, 2, os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            var sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Айгерим Бекова");
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("09:00");
            assertThat(sheet.getRow(1).getCell(4).getStringCellValue()).isEqualTo("PRESENT");
        }
    }

    @Test
    void emptyMonth_onlyHeader() throws Exception {
        when(attendanceClient.daily(any())).thenReturn(List.of());

        File out = tmp.resolve("attendance-empty.xlsx").toFile();
        try (OutputStream os = new FileOutputStream(out)) {
            report.write(2026, 2, os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(0);
        }
    }
}
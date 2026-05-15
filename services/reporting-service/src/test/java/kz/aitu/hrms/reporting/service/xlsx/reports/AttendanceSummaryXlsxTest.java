package kz.aitu.hrms.reporting.service.xlsx.reports;

import kz.aitu.hrms.reporting.client.AttendanceClient;
import kz.aitu.hrms.reporting.client.EmployeeClient;
import kz.aitu.hrms.reporting.client.dto.AttendanceRecordDto;
import kz.aitu.hrms.reporting.client.dto.EmployeeSummaryDto;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceSummaryXlsxTest {

    @Mock AttendanceClient attendanceClient;
    @Mock EmployeeClient employeeClient;
    @InjectMocks AttendanceSummaryXlsx report;

    @TempDir Path tmp;

    @Test
    void writesAttendanceSummaryPerEmployee() throws Exception {
        UUID empId = UUID.randomUUID();
        EmployeeSummaryDto emp = new EmployeeSummaryDto();
        emp.setId(empId);
        emp.setFirstName("Сауле");
        emp.setLastName("Жанова");

        PageResponse<EmployeeSummaryDto> empPage = new PageResponse<>();
        empPage.setContent(List.of(emp));
        when(employeeClient.list(isNull(), eq("ACTIVE"), eq(0), eq(500))).thenReturn(empPage);

        AttendanceRecordDto rec = new AttendanceRecordDto();
        rec.setEmployeeId(empId);
        rec.setStatus("PRESENT");

        PageResponse<AttendanceRecordDto> attPage = new PageResponse<>();
        attPage.setContent(List.of(rec));
        attPage.setLast(true);

        PageResponse<AttendanceRecordDto> empty = new PageResponse<>();
        empty.setContent(List.of());
        empty.setLast(true);

        // Feb 2026 = 28 days; stub first day with record, rest empty
        when(attendanceClient.daily(any(), anyInt(), anyInt())).thenReturn(empty);
        when(attendanceClient.daily(eq("2026-02-01"), eq(0), eq(200))).thenReturn(attPage);

        File out = tmp.resolve("summary.xlsx").toFile();
        try (OutputStream os = new FileOutputStream(out)) {
            report.write(2026, 2, os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            var sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Сауле");
            // 1 PRESENT day
            assertThat(sheet.getRow(1).getCell(2).getNumericCellValue()).isEqualTo(1.0);
        }
    }

    @Test
    void noEmployees_returnsOnlyHeader() throws Exception {
        PageResponse<EmployeeSummaryDto> empty = new PageResponse<>();
        empty.setContent(List.of());
        when(employeeClient.list(isNull(), eq("ACTIVE"), eq(0), eq(500))).thenReturn(empty);

        File out = tmp.resolve("summary-empty.xlsx").toFile();
        try (OutputStream os = new FileOutputStream(out)) {
            report.write(2026, 2, os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(0);
        }
    }
}

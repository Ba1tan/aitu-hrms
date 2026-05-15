package kz.aitu.hrms.reporting.service.xlsx.reports;

import kz.aitu.hrms.reporting.client.EmployeeClient;
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
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeadcountXlsxTest {

    @Mock EmployeeClient employeeClient;
    @InjectMocks HeadcountXlsx report;

    @TempDir Path tmp;

    @Test
    void writesHeadcountByMonth() throws Exception {
        EmployeeSummaryDto emp = new EmployeeSummaryDto();
        emp.setHireDate(LocalDate.of(2025, 12, 15)); // hired before range start → counted for all months

        PageResponse<EmployeeSummaryDto> page = new PageResponse<>();
        page.setContent(List.of(emp));
        page.setLast(true);
        when(employeeClient.list(isNull(), isNull(), eq(0), eq(200))).thenReturn(page);

        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 1);

        File out = tmp.resolve("headcount.xlsx").toFile();
        try (OutputStream os = new FileOutputStream(out)) {
            report.write(from, to, os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            var sheet = wb.getSheetAt(0);
            // 3 rows: Jan, Feb, Mar
            assertThat(sheet.getLastRowNum()).isEqualTo(3);
            // Jan row: hired on Jan 5 → counted for Jan 1
            assertThat(sheet.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(1.0);
        }
    }

    @Test
    void employeeHiredAfterRange_notCounted() throws Exception {
        EmployeeSummaryDto emp = new EmployeeSummaryDto();
        emp.setHireDate(LocalDate.of(2027, 1, 1));

        PageResponse<EmployeeSummaryDto> page = new PageResponse<>();
        page.setContent(List.of(emp));
        page.setLast(true);
        when(employeeClient.list(isNull(), isNull(), eq(0), eq(200))).thenReturn(page);

        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 1);

        File out = tmp.resolve("headcount-future.xlsx").toFile();
        try (OutputStream os = new FileOutputStream(out)) {
            report.write(from, to, os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            assertThat(wb.getSheetAt(0).getRow(1).getCell(1).getNumericCellValue()).isEqualTo(0.0);
        }
    }
}

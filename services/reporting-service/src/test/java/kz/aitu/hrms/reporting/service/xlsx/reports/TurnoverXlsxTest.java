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
class TurnoverXlsxTest {

    @Mock EmployeeClient employeeClient;
    @InjectMocks TurnoverXlsx report;

    @TempDir Path tmp;

    @Test
    void writesTwelveMonthRows() throws Exception {
        EmployeeSummaryDto emp = new EmployeeSummaryDto();
        emp.setHireDate(LocalDate.of(2026, 3, 15));

        PageResponse<EmployeeSummaryDto> page = new PageResponse<>();
        page.setContent(List.of(emp));
        page.setLast(true);
        when(employeeClient.list(isNull(), isNull(), eq(0), eq(200))).thenReturn(page);

        File out = tmp.resolve("turnover.xlsx").toFile();
        try (OutputStream os = new FileOutputStream(out)) {
            report.write(2026, os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            var sheet = wb.getSheetAt(0);
            // 12 month rows after header
            assertThat(sheet.getLastRowNum()).isEqualTo(12);
            // March row (row index 3) should have 1 hired
            assertThat(sheet.getRow(3).getCell(1).getNumericCellValue()).isEqualTo(1.0);
            // January row should have 0
            assertThat(sheet.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(0.0);
        }
    }

    @Test
    void employeeFromDifferentYear_notCounted() throws Exception {
        EmployeeSummaryDto emp = new EmployeeSummaryDto();
        emp.setHireDate(LocalDate.of(2025, 3, 15));

        PageResponse<EmployeeSummaryDto> page = new PageResponse<>();
        page.setContent(List.of(emp));
        page.setLast(true);
        when(employeeClient.list(isNull(), isNull(), eq(0), eq(200))).thenReturn(page);

        File out = tmp.resolve("turnover-wrong-year.xlsx").toFile();
        try (OutputStream os = new FileOutputStream(out)) {
            report.write(2026, os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            var sheet = wb.getSheetAt(0);
            // All months should have 0 hired
            assertThat(sheet.getRow(3).getCell(1).getNumericCellValue()).isEqualTo(0.0);
        }
    }
}

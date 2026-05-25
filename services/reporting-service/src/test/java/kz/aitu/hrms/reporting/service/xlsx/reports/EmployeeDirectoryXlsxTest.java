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
class EmployeeDirectoryXlsxTest {

    @Mock EmployeeClient employeeClient;
    @InjectMocks EmployeeDirectoryXlsx report;

    @TempDir Path tmp;

    @Test
    void writesEmployees() throws Exception {
        EmployeeSummaryDto emp = new EmployeeSummaryDto();
        emp.setFullName("Нурлан Сейткали");
        emp.setPosition("Инженер");
        emp.setDepartment("IT");
        emp.setStatus("ACTIVE");
        emp.setHireDate(LocalDate.of(2023, 3, 1));

        PageResponse<EmployeeSummaryDto> page = new PageResponse<>();
        page.setContent(List.of(emp));
        page.setLast(true);

        when(employeeClient.list(any(), any(), anyInt(), anyInt())).thenReturn(page);

        File out = tmp.resolve("dir.xlsx").toFile();
        try (OutputStream os = new FileOutputStream(out)) {
            report.write(os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            var sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Нурлан Сейткали");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Инженер");
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).isEqualTo("ACTIVE");
        }
    }
}

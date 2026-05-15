package kz.aitu.hrms.reporting.service.xlsx.reports;

import kz.aitu.hrms.reporting.client.PayrollClient;
import kz.aitu.hrms.reporting.client.dto.PageResponse;
import kz.aitu.hrms.reporting.client.dto.PayslipDto;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollSummaryXlsxTest {

    @Mock PayrollClient payrollClient;
    @InjectMocks PayrollSummaryXlsx report;

    @TempDir Path tmp;

    @Test
    void writesPayslipsToXlsx() throws Exception {
        PayslipDto slip = new PayslipDto();
        slip.setEmployeeFirstName("Алия");
        slip.setEmployeeLastName("Иванова");
        slip.setGrossSalary(BigDecimal.valueOf(500000));
        slip.setOpv(BigDecimal.valueOf(50000));
        slip.setVosms(BigDecimal.valueOf(2000));
        slip.setIpn(BigDecimal.valueOf(30000));
        slip.setNetSalary(BigDecimal.valueOf(418000));

        PageResponse<PayslipDto> page = new PageResponse<>();
        page.setContent(List.of(slip));
        page.setLast(true);

        when(payrollClient.listPayslips(any(UUID.class), eq(0), eq(200))).thenReturn(page);

        File out = tmp.resolve("payroll.xlsx").toFile();
        try (OutputStream os = new FileOutputStream(out)) {
            report.write(UUID.randomUUID(), os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            var sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Сотрудник");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Алия");
            assertThat(sheet.getRow(1).getCell(2).getNumericCellValue()).isEqualTo(500000.0);
        }
    }

    @Test
    void emptyPeriod_onlyHeader() throws Exception {
        PageResponse<PayslipDto> page = new PageResponse<>();
        page.setContent(List.of());
        page.setLast(true);
        when(payrollClient.listPayslips(any(UUID.class), eq(0), eq(200))).thenReturn(page);

        File out = tmp.resolve("empty.xlsx").toFile();
        try (OutputStream os = new FileOutputStream(out)) {
            report.write(UUID.randomUUID(), os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(0);
        }
    }
}

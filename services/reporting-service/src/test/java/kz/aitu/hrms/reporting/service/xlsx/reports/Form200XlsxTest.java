package kz.aitu.hrms.reporting.service.xlsx.reports;

import kz.aitu.hrms.reporting.client.PayrollClient;
import kz.aitu.hrms.reporting.client.dto.PageResponse;
import kz.aitu.hrms.reporting.client.dto.PayrollPeriodDto;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Form200XlsxTest {

    @Mock PayrollClient payrollClient;
    @InjectMocks Form200Xlsx report;

    @TempDir Path tmp;

    @Test
    void writesMatchingPeriodPayslips() throws Exception {
        PayrollPeriodDto period = new PayrollPeriodDto();
        period.setId(UUID.randomUUID());
        period.setStartDate(LocalDate.of(2026, 1, 1));

        PageResponse<PayrollPeriodDto> periods = new PageResponse<>();
        periods.setContent(List.of(period));
        periods.setLast(true);
        when(payrollClient.listPeriods(0, 50)).thenReturn(periods);

        PayslipDto slip = new PayslipDto();
        slip.setEmployeeFirstName("Алия");
        slip.setEmployeeLastName("Тест");
        slip.setGrossSalary(BigDecimal.valueOf(500000));
        slip.setOpv(BigDecimal.valueOf(50000));
        slip.setIpn(BigDecimal.valueOf(30000));
        slip.setNetSalary(BigDecimal.valueOf(420000));

        PageResponse<PayslipDto> slips = new PageResponse<>();
        slips.setContent(List.of(slip));
        slips.setLast(true);
        when(payrollClient.listPayslips(eq(period.getId()), eq(0), eq(200))).thenReturn(slips);

        File out = tmp.resolve("form200.xlsx").toFile();
        try (OutputStream os = new FileOutputStream(out)) {
            report.write(2026, 1, os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            var sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("Алия");
        }
    }

    @Test
    void noMatchingPeriods_onlyHeader() throws Exception {
        PayrollPeriodDto period = new PayrollPeriodDto();
        period.setId(UUID.randomUUID());
        period.setStartDate(LocalDate.of(2025, 7, 1)); // Q3 2025, not Q1 2026

        PageResponse<PayrollPeriodDto> periods = new PageResponse<>();
        periods.setContent(List.of(period));
        periods.setLast(true);
        when(payrollClient.listPeriods(0, 50)).thenReturn(periods);

        File out = tmp.resolve("form200-empty.xlsx").toFile();
        try (OutputStream os = new FileOutputStream(out)) {
            report.write(2026, 1, os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(0);
        }
    }

    @Test
    void nullPeriods_returnsEmptySheet() throws Exception {
        PageResponse<PayrollPeriodDto> periods = new PageResponse<>();
        periods.setContent(null);
        periods.setLast(true);
        when(payrollClient.listPeriods(0, 50)).thenReturn(periods);

        File out = tmp.resolve("form200-null.xlsx").toFile();
        try (OutputStream os = new FileOutputStream(out)) {
            report.write(2026, 1, os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(0);
        }
    }
}

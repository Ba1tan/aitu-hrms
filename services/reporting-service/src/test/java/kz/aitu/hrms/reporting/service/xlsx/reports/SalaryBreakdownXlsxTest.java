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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalaryBreakdownXlsxTest {

    @Mock PayrollClient payrollClient;
    @InjectMocks SalaryBreakdownXlsx report;

    @TempDir Path tmp;

    @Test
    void writesPayslipsFromLatestPeriod() throws Exception {
        PayrollPeriodDto period = new PayrollPeriodDto();
        period.setId(UUID.randomUUID());
        period.setName("Май 2026");
        when(payrollClient.getLatestPeriod()).thenReturn(period);

        PayslipDto slip = new PayslipDto();
        slip.setEmployeeFirstName("Нурлан");
        slip.setEmployeeLastName("Сейткали");
        slip.setPeriodName("Май 2026");
        slip.setGrossSalary(BigDecimal.valueOf(400000));
        slip.setOpv(BigDecimal.valueOf(40000));
        slip.setVosms(BigDecimal.valueOf(2000));
        slip.setIpn(BigDecimal.valueOf(25000));
        slip.setNetSalary(BigDecimal.valueOf(333000));

        PageResponse<PayslipDto> page = new PageResponse<>();
        page.setContent(List.of(slip));
        page.setLast(true);
        when(payrollClient.listPayslips(eq(period.getId()), eq(0), eq(200))).thenReturn(page);

        File out = tmp.resolve("breakdown.xlsx").toFile();
        try (OutputStream os = new FileOutputStream(out)) {
            report.write(null, os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            var sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Нурлан");
        }
    }

    @Test
    void fallsBackToListPeriods_whenGetLatestFails() throws Exception {
        when(payrollClient.getLatestPeriod()).thenThrow(new RuntimeException("unavailable"));

        PayrollPeriodDto period = new PayrollPeriodDto();
        period.setId(UUID.randomUUID());
        PageResponse<PayrollPeriodDto> pg = new PageResponse<>();
        pg.setContent(List.of(period));
        pg.setLast(true);
        when(payrollClient.listPeriods(0, 1)).thenReturn(pg);

        PageResponse<PayslipDto> slips = new PageResponse<>();
        slips.setContent(List.of());
        slips.setLast(true);
        when(payrollClient.listPayslips(eq(period.getId()), eq(0), eq(200))).thenReturn(slips);

        File out = tmp.resolve("breakdown-fallback.xlsx").toFile();
        try (OutputStream os = new FileOutputStream(out)) {
            report.write(null, os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(0);
        }
    }

    @Test
    void noPeriodAvailable_returnsOnlyHeader() throws Exception {
        when(payrollClient.getLatestPeriod()).thenThrow(new RuntimeException("down"));
        PageResponse<PayrollPeriodDto> empty = new PageResponse<>();
        empty.setContent(List.of());
        empty.setLast(true);
        when(payrollClient.listPeriods(0, 1)).thenReturn(empty);

        File out = tmp.resolve("breakdown-noperiod.xlsx").toFile();
        try (OutputStream os = new FileOutputStream(out)) {
            report.write(null, os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(0);
        }
    }
}

package kz.aitu.hrms.reporting.service.xlsx.reports;

import kz.aitu.hrms.reporting.client.EmployeeClient;
import kz.aitu.hrms.reporting.client.LeaveClient;
import kz.aitu.hrms.reporting.client.dto.EmployeeSummaryDto;
import kz.aitu.hrms.reporting.client.dto.LeaveBalanceDto;
import kz.aitu.hrms.reporting.client.dto.PageResponse;
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
class LeaveBalancesXlsxTest {

    @Mock EmployeeClient employeeClient;
    @Mock LeaveClient leaveClient;
    @InjectMocks LeaveBalancesXlsx report;

    @TempDir Path tmp;

    @Test
    void writesLeaveBalancesForEmployees() throws Exception {
        UUID empId = UUID.randomUUID();
        EmployeeSummaryDto emp = new EmployeeSummaryDto();
        emp.setId(empId);
        emp.setFullName("Данияр Нуров");

        PageResponse<EmployeeSummaryDto> empPage = new PageResponse<>();
        empPage.setContent(List.of(emp));
        empPage.setLast(true);
        when(employeeClient.list(isNull(), eq("ACTIVE"), eq(0), eq(500))).thenReturn(empPage);

        LeaveBalanceDto.LeaveTypeInfo type = new LeaveBalanceDto.LeaveTypeInfo();
        type.setName("ANNUAL");
        LeaveBalanceDto balance = new LeaveBalanceDto();
        balance.setLeaveType(type);
        balance.setEntitledDays(BigDecimal.valueOf(28));
        balance.setUsedDays(BigDecimal.valueOf(10));
        balance.setRemainingDays(BigDecimal.valueOf(18));
        when(leaveClient.balancesFor(empId, 2026)).thenReturn(List.of(balance));

        File out = tmp.resolve("leave.xlsx").toFile();
        try (OutputStream os = new FileOutputStream(out)) {
            report.write(2026, os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            var sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Данияр Нуров");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("ANNUAL");
        }
    }

    @Test
    void employeeWithNoBalances_writesPlaceholderRow() throws Exception {
        UUID empId = UUID.randomUUID();
        EmployeeSummaryDto emp = new EmployeeSummaryDto();
        emp.setId(empId);
        emp.setFullName("Айна Сат");

        PageResponse<EmployeeSummaryDto> empPage = new PageResponse<>();
        empPage.setContent(List.of(emp));
        empPage.setLast(true);
        when(employeeClient.list(isNull(), eq("ACTIVE"), eq(0), eq(500))).thenReturn(empPage);
        when(leaveClient.balancesFor(empId, 2026)).thenReturn(List.of());

        File out = tmp.resolve("leave-empty-balance.xlsx").toFile();
        try (OutputStream os = new FileOutputStream(out)) {
            report.write(2026, os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            var sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("—");
        }
    }

    @Test
    void leaveClientFails_writesErrorRow() throws Exception {
        UUID empId = UUID.randomUUID();
        EmployeeSummaryDto emp = new EmployeeSummaryDto();
        emp.setId(empId);
        emp.setFullName("Тест Ошибка");

        PageResponse<EmployeeSummaryDto> empPage = new PageResponse<>();
        empPage.setContent(List.of(emp));
        empPage.setLast(true);
        when(employeeClient.list(isNull(), eq("ACTIVE"), eq(0), eq(500))).thenReturn(empPage);
        when(leaveClient.balancesFor(any(), anyInt())).thenThrow(new RuntimeException("down"));

        File out = tmp.resolve("leave-error.xlsx").toFile();
        try (OutputStream os = new FileOutputStream(out)) {
            report.write(2026, os);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(out)) {
            assertThat(wb.getSheetAt(0).getRow(1).getCell(1).getStringCellValue()).isEqualTo("Ошибка");
        }
    }
}

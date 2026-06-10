package kz.aitu.hrms.integration.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.integration.client.EmployeeClient;
import kz.aitu.hrms.integration.client.PayrollClient;
import kz.aitu.hrms.integration.client.dto.EmployeeIinDto;
import kz.aitu.hrms.integration.client.dto.PageResponse;
import kz.aitu.hrms.integration.client.dto.PayslipDetailDto;
import kz.aitu.hrms.integration.service.bank.KaspiTsvGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankFileServiceTest {

    @Mock private PayrollClient payrollClient;
    @Mock private EmployeeClient employeeClient;
    @Mock private SettingsService settings;
    @Mock private AuditPublisher auditPublisher;

    private BankFileService service(java.util.List<kz.aitu.hrms.integration.service.bank.BankFileGenerator> gens) {
        return new BankFileService(gens, payrollClient, employeeClient, settings, auditPublisher);
    }

    @Test
    void generate_pullsIbanFromEmployeeService_andAudits() throws Exception {
        UUID periodId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();

        PayslipDetailDto p = new PayslipDetailDto();
        PayslipDetailDto.EmployeeInfo emp = new PayslipDetailDto.EmployeeInfo();
        emp.setId(empId);
        emp.setIin("900101300700");
        emp.setFullName("Иванов Иван");
        p.setEmployee(emp);
        PayslipDetailDto.PeriodInfo per = new PayslipDetailDto.PeriodInfo();
        per.setYear(2026);
        per.setMonth(3);
        p.setPeriod(per);
        p.setNetSalary(new BigDecimal("250000"));

        PageResponse<PayslipDetailDto> page = new PageResponse<>();
        page.setContent(List.of(p));
        when(payrollClient.listPayslipsForPeriod(eq(periodId), anyInt(), anyInt())).thenReturn(page);

        EmployeeIinDto bank = new EmployeeIinDto();
        bank.setBankAccount("KZ75125KZT2069100100");
        bank.setBankName("Kaspi");
        when(employeeClient.getById(empId)).thenReturn(bank);

        when(settings.snapshot()).thenReturn(new CompanySettingSnapshot("123456789012", "ТОО Тест", "17"));
        when(settings.getOrDefault(eq("integration.bank_knp"), anyString())).thenReturn("010");
        when(settings.getOrDefault(eq("integration.bank_kbe"), anyString())).thenReturn("19");
        when(settings.getOrDefault(eq("integration.bank_payment_purpose"), anyString())).thenReturn("");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service(List.of(new KaspiTsvGenerator())).generate(periodId, "KASPI_TSV", out);

        String s = out.toString(StandardCharsets.UTF_8);
        assertThat(s).contains("KZ75125KZT2069100100").contains("250000.00");
        assertThat(s).contains("Заработная плата за 03.2026");
        verify(auditPublisher).audit(eq("BANK_FILE"), eq("BANK_FILE"), eq(periodId), isNull(), any());
    }

    @Test
    void generate_unsupportedFormat_throws() {
        assertThatThrownBy(() ->
                service(List.of()).generate(UUID.randomUUID(), "NOPE", new ByteArrayOutputStream()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Unsupported bank format");
    }
}

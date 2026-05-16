package kz.aitu.hrms.integration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.integration.client.PayrollClient;
import kz.aitu.hrms.integration.client.dto.EmployeeIinDto;
import kz.aitu.hrms.integration.client.dto.PayrollPeriodDto;
import kz.aitu.hrms.integration.client.dto.PayslipDetailDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OneCPayloadBuilderTest {

    @Mock private SettingsService settings;
    @Mock private PayrollClient payrollClient;

    private OneCPayloadBuilder builder;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private UUID periodId;
    private UUID employeeId;

    @BeforeEach
    void setup() throws Exception {
        builder = new OneCPayloadBuilder(settings, payrollClient, objectMapper);

        periodId   = UUID.randomUUID();
        employeeId = UUID.randomUUID();

        when(settings.getRequired("company.bin")).thenReturn("123456789012");
        when(settings.getRequired("company.name")).thenReturn("ТОО Компания");
        when(settings.getOrDefault("company.kbe", "17")).thenReturn("17");

        PayrollPeriodDto period = new PayrollPeriodDto();
        period.setId(periodId);
        period.setYear(2026);
        period.setMonth(3);
        when(payrollClient.getPeriod(periodId)).thenReturn(period);
    }

    @Test
    void build_producesCorrectJsonShape() throws Exception {
        PayslipDetailDto payslip = buildPayslip(employeeId,
                BigDecimal.valueOf(300000), BigDecimal.valueOf(300000),
                BigDecimal.valueOf(30000), BigDecimal.valueOf(6000),
                BigDecimal.valueOf(13425), BigDecimal.valueOf(250575),
                BigDecimal.valueOf(13500), BigDecimal.valueOf(18000),
                BigDecimal.valueOf(10500), 22, 22, true, false);

        EmployeeIinDto emp = new EmployeeIinDto();
        emp.setEmployeeId(employeeId);
        emp.setIin("123456789012");
        emp.setFullName("Иванов Иван Иванович");

        String json = builder.build(periodId, List.of(payslip), List.of(emp));
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("period").get("year").asInt()).isEqualTo(2026);
        assertThat(root.get("period").get("month").asInt()).isEqualTo(3);
        assertThat(root.get("period").get("startDate").asText()).isEqualTo("2026-03-01");
        assertThat(root.get("period").get("endDate").asText()).isEqualTo("2026-03-31");

        assertThat(root.get("organization").get("bin").asText()).isEqualTo("123456789012");
        assertThat(root.get("organization").get("name").asText()).isEqualTo("ТОО Компания");
        assertThat(root.get("organization").get("kbe").asText()).isEqualTo("17");

        JsonNode emp0 = root.get("employees").get(0);
        assertThat(emp0.get("iin").asText()).isEqualTo("123456789012");
        assertThat(emp0.get("fullName").asText()).isEqualTo("Иванов Иван Иванович");
        assertThat(emp0.get("grossSalary").decimalValue()).isEqualByComparingTo("300000");
        assertThat(emp0.get("netSalary").decimalValue()).isEqualByComparingTo("250575");
        assertThat(emp0.get("opvAmount").decimalValue()).isEqualByComparingTo("30000");
        assertThat(emp0.get("workedDays").asInt()).isEqualTo(22);
        assertThat(emp0.get("isResident").asBoolean()).isTrue();
        assertThat(emp0.get("hasDisability").asBoolean()).isFalse();

        assertThat(root.get("totals").get("totalGross").decimalValue())
                .isEqualByComparingTo("300000");
        assertThat(root.get("totals").get("employeeCount").asInt()).isEqualTo(1);
    }

    @Test
    void build_missingIin_throwsBusinessException() {
        PayslipDetailDto payslip = buildPayslip(employeeId,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(100000),
                BigDecimal.valueOf(10000), BigDecimal.valueOf(2000),
                BigDecimal.valueOf(4000), BigDecimal.valueOf(84000),
                BigDecimal.valueOf(4500), BigDecimal.valueOf(6000),
                BigDecimal.valueOf(3500), 20, 20, true, false);

        EmployeeIinDto emp = new EmployeeIinDto();
        emp.setEmployeeId(employeeId);
        emp.setIin("");  // empty IIN
        emp.setFullName("Test User");

        assertThatThrownBy(() -> builder.build(periodId, List.of(payslip), List.of(emp)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("has no IIN");
    }

    @Test
    void build_totalsComputedFromEmployeeRows() throws Exception {
        UUID emp2Id = UUID.randomUUID();
        PayslipDetailDto p1 = buildPayslip(employeeId,
                BigDecimal.valueOf(200000), BigDecimal.valueOf(200000),
                BigDecimal.valueOf(20000), BigDecimal.valueOf(4000),
                BigDecimal.valueOf(8950), BigDecimal.valueOf(167050),
                BigDecimal.valueOf(9000), BigDecimal.valueOf(12000),
                BigDecimal.valueOf(7000), 22, 22, true, false);
        PayslipDetailDto p2 = buildPayslip(emp2Id,
                BigDecimal.valueOf(300000), BigDecimal.valueOf(300000),
                BigDecimal.valueOf(30000), BigDecimal.valueOf(6000),
                BigDecimal.valueOf(13425), BigDecimal.valueOf(250575),
                BigDecimal.valueOf(13500), BigDecimal.valueOf(18000),
                BigDecimal.valueOf(10500), 22, 22, true, false);

        EmployeeIinDto e1 = new EmployeeIinDto();
        e1.setEmployeeId(employeeId); e1.setIin("111111111111"); e1.setFullName("User One");
        EmployeeIinDto e2 = new EmployeeIinDto();
        e2.setEmployeeId(emp2Id); e2.setIin("222222222222"); e2.setFullName("User Two");

        String json = builder.build(periodId, List.of(p1, p2), List.of(e1, e2));
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("totals").get("totalGross").decimalValue())
                .isEqualByComparingTo("500000");
        assertThat(root.get("totals").get("employeeCount").asInt()).isEqualTo(2);
    }

    private PayslipDetailDto buildPayslip(UUID empId,
            BigDecimal gross, BigDecimal earned, BigDecimal opv, BigDecimal vosms,
            BigDecimal ipn, BigDecimal net, BigDecimal so, BigDecimal sn, BigDecimal opvr,
            int worked, int total, boolean resident, boolean disability) {
        PayslipDetailDto p = new PayslipDetailDto();
        PayslipDetailDto.EmployeeInfo emp = new PayslipDetailDto.EmployeeInfo();
        emp.setId(empId);
        p.setEmployee(emp);
        p.setGrossSalary(gross);
        p.setEarnedSalary(earned);
        p.setOpvAmount(opv);
        p.setVosmsAmount(vosms);
        p.setIpnAmount(ipn);
        p.setNetSalary(net);
        p.setSoAmount(so);
        p.setSnAmount(sn);
        p.setOpvrAmount(opvr);
        p.setWorkedDays(worked);
        p.setTotalWorkingDays(total);
        p.setResident(resident);
        p.setHasDisability(disability);
        return p;
    }
}

package kz.aitu.hrms.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.integration.client.PayrollClient;
import kz.aitu.hrms.integration.client.dto.EmployeeIinDto;
import kz.aitu.hrms.integration.client.dto.PayrollPeriodDto;
import kz.aitu.hrms.integration.client.dto.PayslipDetailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OneCPayloadBuilder {

    private final SettingsService settings;
    private final PayrollClient payrollClient;
    private final ObjectMapper objectMapper;

    public String build(UUID periodId,
                        List<PayslipDetailDto> payslips,
                        List<EmployeeIinDto> employees) {
        var iinMap = employees.stream()
                .collect(Collectors.toMap(EmployeeIinDto::getEmployeeId, e -> e));

        String bin  = settings.getRequired("company.bin");
        String name = settings.getRequired("company.name");
        String kbe  = settings.getOrDefault("company.kbe", "17");

        PayrollPeriodDto period = payrollClient.getPeriod(periodId);
        int year  = period.getYear();
        int month = period.getMonth();
        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate   = ym.atEndOfMonth();

        List<Map<String, Object>> employeeRows = new ArrayList<>();
        for (PayslipDetailDto p : payslips) {
            UUID empId = p.getEmployeeId();
            EmployeeIinDto emp = iinMap.get(empId);
            if (emp == null || emp.getIin() == null || emp.getIin().isBlank()) {
                throw new BusinessException("Employee " + empId + " has no IIN in employee-service");
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("iin",              emp.getIin());
            row.put("fullName",         emp.getFullName());
            row.put("grossSalary",      p.getGrossSalary());
            row.put("earnedSalary",     p.getEarnedSalary());
            row.put("opvAmount",        p.getOpvAmount());
            row.put("vosmsAmount",      p.getVosmsAmount());
            row.put("ipnAmount",        p.getIpnAmount());
            row.put("netSalary",        p.getNetSalary());
            row.put("soAmount",         p.getSoAmount());
            row.put("snAmount",         p.getSnAmount());
            row.put("opvrAmount",       p.getOpvrAmount());
            row.put("workedDays",       p.getWorkedDays());
            row.put("totalWorkingDays", p.getTotalWorkingDays());
            row.put("isResident",       p.isResident());
            row.put("hasDisability",    p.isHasDisability());
            employeeRows.add(row);
        }

        Map<String, Object> totals = computeTotals(payslips, employeeRows.size());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("period", Map.of(
                "year",      year,
                "month",     month,
                "startDate", startDate.toString(),
                "endDate",   endDate.toString()
        ));
        payload.put("organization", Map.of(
                "bin",  bin,
                "name", name,
                "kbe",  kbe
        ));
        payload.put("employees", employeeRows);
        payload.put("totals", totals);

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new BusinessException("Failed to serialize 1C payload: " + e.getMessage());
        }
    }

    private Map<String, Object> computeTotals(List<PayslipDetailDto> payslips, int count) {
        BigDecimal totalGross = sum(payslips, PayslipDetailDto::getGrossSalary);
        BigDecimal totalNet   = sum(payslips, PayslipDetailDto::getNetSalary);
        BigDecimal totalOpv   = sum(payslips, PayslipDetailDto::getOpvAmount);
        BigDecimal totalVosms = sum(payslips, PayslipDetailDto::getVosmsAmount);
        BigDecimal totalIpn   = sum(payslips, PayslipDetailDto::getIpnAmount);
        BigDecimal totalSo    = sum(payslips, PayslipDetailDto::getSoAmount);
        BigDecimal totalSn    = sum(payslips, PayslipDetailDto::getSnAmount);
        BigDecimal totalOpvr  = sum(payslips, PayslipDetailDto::getOpvrAmount);

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("totalGross",     totalGross);
        totals.put("totalNet",       totalNet);
        totals.put("totalOpv",       totalOpv);
        totals.put("totalVosms",     totalVosms);
        totals.put("totalIpn",       totalIpn);
        totals.put("totalSo",        totalSo);
        totals.put("totalSn",        totalSn);
        totals.put("totalOpvr",      totalOpvr);
        totals.put("employeeCount",  count);
        return totals;
    }

    private BigDecimal sum(List<PayslipDetailDto> list,
                           java.util.function.Function<PayslipDetailDto, BigDecimal> extractor) {
        return list.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

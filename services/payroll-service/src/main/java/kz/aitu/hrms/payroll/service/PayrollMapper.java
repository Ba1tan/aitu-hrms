package kz.aitu.hrms.payroll.service;

import kz.aitu.hrms.payroll.dto.AdditionDtos;
import kz.aitu.hrms.payroll.dto.PayslipDtos;
import kz.aitu.hrms.payroll.dto.PeriodDtos;
import kz.aitu.hrms.payroll.entity.PayrollAddition;
import kz.aitu.hrms.payroll.entity.PayrollPeriod;
import kz.aitu.hrms.payroll.entity.Payslip;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Arrays;
import java.util.List;

@Mapper(componentModel = "spring")
public interface PayrollMapper {

    // ── Period ─────────────────────────────────────────────────────────────
    @Mapping(target = "name", source = "name")
    @Mapping(target = "summary", ignore = true)
    PeriodDtos.Response toPeriodResponse(PayrollPeriod entity);

    // ── Payslip ────────────────────────────────────────────────────────────
    @Mapping(target = "period",   source = "period",   qualifiedByName = "toPeriodInfo")
    @Mapping(target = "employee", source = ".",        qualifiedByName = "toEmployeeInfo")
    @Mapping(target = "isResident",     source = "resident")
    @Mapping(target = "hasDisability",  source = "disability")
    @Mapping(target = "anomalyFlags",   source = "anomalyFlags", qualifiedByName = "toFlagList")
    PayslipDtos.Response toPayslipResponse(Payslip entity);

    @Named("toPeriodInfo")
    default PayslipDtos.PeriodInfo toPeriodInfo(PayrollPeriod p) {
        if (p == null) return null;
        return PayslipDtos.PeriodInfo.builder()
                .id(p.getId())
                .year(p.getYear())
                .month(p.getMonth())
                .name(p.getName())
                .build();
    }

    @Named("toEmployeeInfo")
    default PayslipDtos.EmployeeInfo toEmployeeInfo(Payslip s) {
        return PayslipDtos.EmployeeInfo.builder()
                .id(s.getEmployeeId())
                .employeeNumber(s.getEmployeeNumber())
                .fullName(s.getEmployeeName())
                .iin(s.getEmployeeIin())
                .department(s.getDepartmentName())
                .position(s.getPositionTitle())
                .build();
    }

    @Named("toFlagList")
    default List<String> toFlagList(String csv) {
        if (csv == null || csv.isBlank()) return null;
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    // ── Addition ───────────────────────────────────────────────────────────
    @Mapping(target = "isTaxable", source = "taxable")
    AdditionDtos.Response toAdditionResponse(PayrollAddition entity);
}
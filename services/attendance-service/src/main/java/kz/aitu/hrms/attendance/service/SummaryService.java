package kz.aitu.hrms.attendance.service;

import kz.aitu.hrms.attendance.dto.SummaryDtos;

import java.util.UUID;

public interface SummaryService {

    SummaryDtos.EmployeeSummary employeeSummary(UUID employeeId, int year, int month);

    SummaryDtos.AggregateSummary departmentSummary(UUID departmentId, int year, int month);

    SummaryDtos.AggregateSummary companySummary(int year, int month);
}
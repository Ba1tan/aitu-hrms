package kz.aitu.hrms.employee.service;

import kz.aitu.hrms.employee.dto.SalaryHistoryDtos;

import java.util.List;
import java.util.UUID;

public interface SalaryHistoryService {

    List<SalaryHistoryDtos.SalaryHistoryResponse> listForEmployee(UUID employeeId);

    SalaryHistoryDtos.SalaryHistoryResponse recordChange(UUID employeeId,
                                                        SalaryHistoryDtos.SalaryChangeRequest req);
}
package kz.aitu.hrms.employee.service;

import kz.aitu.hrms.employee.dto.EmployeeDtos;

import java.util.List;
import java.util.UUID;

public interface OrgChartService {

    List<EmployeeDtos.OrgChartNode> fullChart();

    EmployeeDtos.OrgChartNode subtree(UUID rootEmployeeId);
}
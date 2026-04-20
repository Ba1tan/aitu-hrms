package kz.aitu.hrms.employee.service;

import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.employee.dto.EmployeeDtos;
import kz.aitu.hrms.employee.entity.Employee;
import kz.aitu.hrms.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrgChartServiceImpl implements OrgChartService {

    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeDtos.OrgChartNode> fullChart() {
        List<Employee> all = employeeRepository.findAllByDeletedFalse();
        Map<UUID, List<Employee>> byManager = new HashMap<>();
        for (Employee e : all) {
            UUID mgr = e.getManager() != null ? e.getManager().getId() : null;
            byManager.computeIfAbsent(mgr, k -> new ArrayList<>()).add(e);
        }
        List<Employee> roots = byManager.getOrDefault(null, List.of());
        return roots.stream()
                .sorted(Comparator.comparing(Employee::getLastName))
                .map(e -> buildNode(e, byManager))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDtos.OrgChartNode subtree(UUID rootEmployeeId) {
        Employee root = employeeRepository.findByIdAndDeletedFalse(rootEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + rootEmployeeId));
        List<Employee> all = employeeRepository.findAllByDeletedFalse();
        Map<UUID, List<Employee>> byManager = new HashMap<>();
        for (Employee e : all) {
            UUID mgr = e.getManager() != null ? e.getManager().getId() : null;
            byManager.computeIfAbsent(mgr, k -> new ArrayList<>()).add(e);
        }
        return buildNode(root, byManager);
    }

    private EmployeeDtos.OrgChartNode buildNode(Employee e, Map<UUID, List<Employee>> byManager) {
        List<EmployeeDtos.OrgChartNode> reports = byManager.getOrDefault(e.getId(), List.of()).stream()
                .sorted(Comparator.comparing(Employee::getLastName))
                .map(child -> buildNode(child, byManager))
                .toList();
        return EmployeeDtos.OrgChartNode.builder()
                .id(e.getId())
                .employeeNumber(e.getEmployeeNumber())
                .fullName(e.getFullName())
                .email(e.getEmail())
                .position(e.getPosition() != null ? e.getPosition().getTitle() : null)
                .department(e.getDepartment() != null ? e.getDepartment().getName() : null)
                .reports(reports)
                .build();
    }
}
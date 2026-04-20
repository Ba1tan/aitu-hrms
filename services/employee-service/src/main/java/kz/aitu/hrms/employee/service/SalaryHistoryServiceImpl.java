package kz.aitu.hrms.employee.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.employee.dto.SalaryHistoryDtos;
import kz.aitu.hrms.employee.entity.Employee;
import kz.aitu.hrms.employee.entity.SalaryHistory;
import kz.aitu.hrms.employee.event.SalaryChangedEvent;
import kz.aitu.hrms.employee.repository.EmployeeRepository;
import kz.aitu.hrms.employee.repository.SalaryHistoryRepository;
import kz.aitu.hrms.employee.security.EmployeeAccessControl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalaryHistoryServiceImpl implements SalaryHistoryService {

    private final SalaryHistoryRepository historyRepository;
    private final EmployeeRepository employeeRepository;
    private final EventPublisher eventPublisher;
    private final EmployeeAccessControl accessControl;
    private final EmployeeMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<SalaryHistoryDtos.SalaryHistoryResponse> listForEmployee(UUID employeeId) {
        Employee emp = requireEmployee(employeeId);
        accessControl.assertCanView(emp);
        return historyRepository.findAllByEmployee_IdAndDeletedFalseOrderByEffectiveDateDesc(employeeId).stream()
                .map(mapper::toSalaryHistoryResponse)
                .toList();
    }

    @Override
    @Transactional
    public SalaryHistoryDtos.SalaryHistoryResponse recordChange(UUID employeeId,
                                                                SalaryHistoryDtos.SalaryChangeRequest req) {
        Employee emp = requireEmployee(employeeId);
        if (req.getNewSalary().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Salary must be non-negative");
        }
        BigDecimal previous = emp.getBaseSalary();

        SalaryHistory entry = SalaryHistory.builder()
                .employee(emp)
                .previousSalary(previous)
                .newSalary(req.getNewSalary())
                .effectiveDate(req.getEffectiveDate())
                .reason(req.getReason())
                .approvedBy(accessControl.resolveEmployeeId())
                .build();
        SalaryHistory saved = historyRepository.save(entry);

        emp.setBaseSalary(req.getNewSalary());

        eventPublisher.publish(SalaryChangedEvent.builder()
                .employeeId(emp.getId())
                .previousSalary(previous)
                .newSalary(req.getNewSalary())
                .effectiveDate(req.getEffectiveDate())
                .reason(req.getReason())
                .build());

        log.info("Salary changed for {}: {} → {}", emp.getEmployeeNumber(), previous, req.getNewSalary());
        return mapper.toSalaryHistoryResponse(saved);
    }

    private Employee requireEmployee(UUID id) {
        return employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }
}
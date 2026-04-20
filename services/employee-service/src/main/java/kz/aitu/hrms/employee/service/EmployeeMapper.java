package kz.aitu.hrms.employee.service;

import kz.aitu.hrms.employee.dto.DepartmentDtos;
import kz.aitu.hrms.employee.dto.EmergencyContactDtos;
import kz.aitu.hrms.employee.dto.EmployeeDocumentDtos;
import kz.aitu.hrms.employee.dto.EmployeeDtos;
import kz.aitu.hrms.employee.dto.PositionDtos;
import kz.aitu.hrms.employee.dto.SalaryHistoryDtos;
import kz.aitu.hrms.employee.entity.Department;
import kz.aitu.hrms.employee.entity.Employee;
import kz.aitu.hrms.employee.entity.EmployeeDocument;
import kz.aitu.hrms.employee.entity.EmergencyContact;
import kz.aitu.hrms.employee.entity.Position;
import kz.aitu.hrms.employee.entity.SalaryHistory;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public EmployeeDtos.EmployeeResponse toResponse(Employee e) {
        if (e == null) return null;
        return EmployeeDtos.EmployeeResponse.builder()
                .id(e.getId())
                .employeeNumber(e.getEmployeeNumber())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .middleName(e.getMiddleName())
                .fullName(e.getFullName())
                .email(e.getEmail())
                .iin(e.getIin())
                .phone(e.getPhone())
                .dateOfBirth(e.getDateOfBirth())
                .gender(e.getGender())
                .hireDate(e.getHireDate())
                .terminationDate(e.getTerminationDate())
                .terminationReason(e.getTerminationReason())
                .status(e.getStatus())
                .employmentType(e.getEmploymentType())
                .baseSalary(e.getBaseSalary())
                .disabilityGroup(e.getDisabilityGroup())
                .isResident(e.isResident())
                .isPensioner(e.isPensioner())
                .address(e.getAddress())
                .department(toDepartmentSummary(e.getDepartment()))
                .position(toPositionSummary(e.getPosition()))
                .manager(toManagerSummary(e.getManager()))
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public EmployeeDtos.EmployeeSummary toSummary(Employee e) {
        return EmployeeDtos.EmployeeSummary.builder()
                .id(e.getId())
                .employeeNumber(e.getEmployeeNumber())
                .fullName(e.getFullName())
                .email(e.getEmail())
                .department(e.getDepartment() != null ? e.getDepartment().getName() : null)
                .position(e.getPosition() != null ? e.getPosition().getTitle() : null)
                .status(e.getStatus())
                .hireDate(e.getHireDate())
                .build();
    }

    public EmployeeDtos.ManagerSummary toManagerSummary(Employee m) {
        if (m == null) return null;
        return EmployeeDtos.ManagerSummary.builder()
                .id(m.getId())
                .fullName(m.getFullName())
                .build();
    }

    public DepartmentDtos.DepartmentSummary toDepartmentSummary(Department d) {
        if (d == null) return null;
        return DepartmentDtos.DepartmentSummary.builder()
                .id(d.getId())
                .name(d.getName())
                .build();
    }

    public DepartmentDtos.DepartmentResponse toDepartmentResponse(Department d, long count) {
        return DepartmentDtos.DepartmentResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .code(d.getCode())
                .description(d.getDescription())
                .parent(toDepartmentSummary(d.getParent()))
                .manager(toManagerSummary(d.getManager()))
                .employeeCount(count)
                .build();
    }

    public PositionDtos.PositionSummary toPositionSummary(Position p) {
        if (p == null) return null;
        return PositionDtos.PositionSummary.builder()
                .id(p.getId())
                .title(p.getTitle())
                .build();
    }

    public PositionDtos.PositionResponse toPositionResponse(Position p) {
        return PositionDtos.PositionResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .minSalary(p.getMinSalary())
                .maxSalary(p.getMaxSalary())
                .description(p.getDescription())
                .department(toDepartmentSummary(p.getDepartment()))
                .build();
    }

    public SalaryHistoryDtos.SalaryHistoryResponse toSalaryHistoryResponse(SalaryHistory h) {
        return SalaryHistoryDtos.SalaryHistoryResponse.builder()
                .id(h.getId())
                .previousSalary(h.getPreviousSalary())
                .newSalary(h.getNewSalary())
                .effectiveDate(h.getEffectiveDate())
                .reason(h.getReason())
                .approvedBy(h.getApprovedBy())
                .createdAt(h.getCreatedAt())
                .build();
    }

    public EmployeeDocumentDtos.DocumentResponse toDocumentResponse(EmployeeDocument d) {
        return EmployeeDocumentDtos.DocumentResponse.builder()
                .id(d.getId())
                .documentType(d.getDocumentType())
                .fileName(d.getFileName())
                .contentType(d.getContentType())
                .fileSize(d.getFileSize())
                .expiryDate(d.getExpiryDate())
                .uploadedAt(d.getCreatedAt())
                .build();
    }

    public EmergencyContactDtos.ContactResponse toContactResponse(EmergencyContact c) {
        return EmergencyContactDtos.ContactResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .relationship(c.getRelationship())
                .phone(c.getPhone())
                .email(c.getEmail())
                .primary(c.isPrimary())
                .build();
    }
}
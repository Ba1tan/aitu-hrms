package kz.aitu.hrms.modules.employee.service;

import kz.aitu.hrms.modules.employee.dto.DepartmentDtos;

import java.util.List;
import java.util.UUID;

public interface DepartmentService {
    DepartmentDtos.DepartmentResponse create(DepartmentDtos.CreateDepartmentRequest request);
    DepartmentDtos.DepartmentResponse getById(UUID id);
    List<DepartmentDtos.DepartmentResponse> getAll();
    DepartmentDtos.DepartmentResponse update(UUID id, DepartmentDtos.UpdateDepartmentRequest request);
    void delete(UUID id);
}

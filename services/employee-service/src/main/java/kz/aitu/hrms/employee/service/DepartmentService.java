package kz.aitu.hrms.employee.service;

import kz.aitu.hrms.employee.dto.DepartmentDtos;

import java.util.List;
import java.util.UUID;

public interface DepartmentService {

    DepartmentDtos.DepartmentResponse create(DepartmentDtos.CreateDepartmentRequest req);

    DepartmentDtos.DepartmentResponse get(UUID id);

    List<DepartmentDtos.DepartmentResponse> list();

    DepartmentDtos.DepartmentResponse update(UUID id, DepartmentDtos.UpdateDepartmentRequest req);

    void delete(UUID id);
}
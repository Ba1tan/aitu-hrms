package kz.aitu.hrms.employee.service;

import kz.aitu.hrms.employee.dto.PositionDtos;

import java.util.List;
import java.util.UUID;

public interface PositionService {

    PositionDtos.PositionResponse create(PositionDtos.CreatePositionRequest req);

    PositionDtos.PositionResponse get(UUID id);

    List<PositionDtos.PositionResponse> list(UUID departmentId);

    PositionDtos.PositionResponse update(UUID id, PositionDtos.UpdatePositionRequest req);

    void delete(UUID id);
}
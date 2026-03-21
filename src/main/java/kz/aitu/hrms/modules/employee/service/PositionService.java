package kz.aitu.hrms.modules.employee.service;

import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.modules.employee.dto.DepartmentDtos;
import kz.aitu.hrms.modules.employee.dto.PositionDtos;
import kz.aitu.hrms.modules.employee.entity.Position;
import kz.aitu.hrms.modules.employee.repository.DepartmentRepository;
import kz.aitu.hrms.modules.employee.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PositionService {

    private final PositionRepository positionRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public PositionDtos.PositionResponse create(PositionDtos.CreatePositionRequest req) {
        Position pos = Position.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .minSalary(req.getMinSalary())
                .maxSalary(req.getMaxSalary())
                .build();
        if (req.getDepartmentId() != null) {
            pos.setDepartment(departmentRepository.findByIdAndDeletedFalse(req.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", req.getDepartmentId())));
        }
        return toResponse(positionRepository.save(pos));
    }

    @Transactional(readOnly = true)
    public List<PositionDtos.PositionResponse> getAll() {
        return positionRepository.findAllByDeletedFalseOrderByTitle()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PositionDtos.PositionResponse> getByDepartment(UUID departmentId) {
        return positionRepository.findByDepartmentIdAndDeletedFalse(departmentId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PositionDtos.PositionResponse getById(UUID id) {
        return toResponse(positionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Position", id)));
    }

    @Transactional
    public PositionDtos.PositionResponse update(UUID id, PositionDtos.UpdatePositionRequest req) {
        Position pos = positionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Position", id));
        if (req.getTitle() != null) pos.setTitle(req.getTitle());
        if (req.getDescription() != null) pos.setDescription(req.getDescription());
        if (req.getMinSalary() != null) pos.setMinSalary(req.getMinSalary());
        if (req.getMaxSalary() != null) pos.setMaxSalary(req.getMaxSalary());
        if (req.getDepartmentId() != null) {
            pos.setDepartment(departmentRepository.findByIdAndDeletedFalse(req.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", req.getDepartmentId())));
        }
        return toResponse(positionRepository.save(pos));
    }

    @Transactional
    public void delete(UUID id) {
        Position pos = positionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Position", id));
        pos.setDeleted(true);
        positionRepository.save(pos);
    }

    private PositionDtos.PositionResponse toResponse(Position p) {
        PositionDtos.PositionResponse r = new PositionDtos.PositionResponse();
        r.setId(p.getId());
        r.setTitle(p.getTitle());
        r.setDescription(p.getDescription());
        r.setMinSalary(p.getMinSalary());
        r.setMaxSalary(p.getMaxSalary());
        if (p.getDepartment() != null) {
            DepartmentDtos.DepartmentResponse dr = new DepartmentDtos.DepartmentResponse();
            dr.setId(p.getDepartment().getId());
            dr.setName(p.getDepartment().getName());
            r.setDepartment(dr);
        }
        return r;
    }
}

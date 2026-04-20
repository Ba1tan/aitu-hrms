package kz.aitu.hrms.employee.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.employee.dto.PositionDtos;
import kz.aitu.hrms.employee.entity.Department;
import kz.aitu.hrms.employee.entity.Position;
import kz.aitu.hrms.employee.repository.DepartmentRepository;
import kz.aitu.hrms.employee.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PositionServiceImpl implements PositionService {

    private final PositionRepository positionRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeMapper mapper;

    @Override
    @Transactional
    public PositionDtos.PositionResponse create(PositionDtos.CreatePositionRequest req) {
        validateBand(req.getMinSalary(), req.getMaxSalary());
        Position pos = Position.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .minSalary(req.getMinSalary())
                .maxSalary(req.getMaxSalary())
                .department(req.getDepartmentId() != null ? requireDepartment(req.getDepartmentId()) : null)
                .build();
        return mapper.toPositionResponse(positionRepository.save(pos));
    }

    @Override
    @Transactional(readOnly = true)
    public PositionDtos.PositionResponse get(UUID id) {
        return mapper.toPositionResponse(requirePosition(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PositionDtos.PositionResponse> list(UUID departmentId) {
        List<Position> positions = departmentId == null
                ? positionRepository.findAllByDeletedFalse()
                : positionRepository.findAllByDepartment_IdAndDeletedFalse(departmentId);
        return positions.stream().map(mapper::toPositionResponse).toList();
    }

    @Override
    @Transactional
    public PositionDtos.PositionResponse update(UUID id, PositionDtos.UpdatePositionRequest req) {
        Position pos = requirePosition(id);
        if (req.getTitle() != null) pos.setTitle(req.getTitle());
        if (req.getDescription() != null) pos.setDescription(req.getDescription());
        if (req.getMinSalary() != null) pos.setMinSalary(req.getMinSalary());
        if (req.getMaxSalary() != null) pos.setMaxSalary(req.getMaxSalary());
        validateBand(pos.getMinSalary(), pos.getMaxSalary());
        if (req.getDepartmentId() != null) pos.setDepartment(requireDepartment(req.getDepartmentId()));
        return mapper.toPositionResponse(pos);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Position pos = requirePosition(id);
        pos.setDeleted(true);
    }

    private void validateBand(BigDecimal min, BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BusinessException("minSalary must not exceed maxSalary");
        }
    }

    private Position requirePosition(UUID id) {
        return positionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found: " + id));
    }

    private Department requireDepartment(UUID id) {
        return departmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
    }
}
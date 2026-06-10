package kz.aitu.hrms.leave.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.leave.dto.LeaveTypeDtos;
import kz.aitu.hrms.leave.entity.LeaveType;
import kz.aitu.hrms.leave.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaveTypeService {

    private final LeaveTypeRepository typeRepo;
    private final LeaveMapper mapper;
    private final EventPublisher events;

    @Transactional(readOnly = true)
    public List<LeaveTypeDtos.Response> list() {
        return typeRepo.findAllByDeletedFalseOrderByName().stream()
                .map(mapper::toType)
                .toList();
    }

    @Transactional
    public LeaveTypeDtos.Response create(LeaveTypeDtos.UpsertRequest req) {
        String code = resolveCode(req.getCode(), req.getName());
        if (typeRepo.existsByCodeAndDeletedFalse(code)) {
            throw new BusinessException("Leave type with code already exists: " + code);
        }
        LeaveType type = LeaveType.builder()
                .name(req.getName())
                .code(code)
                .daysAllowed(req.getDaysAllowed())
                .isPaid(req.getIsPaid() == null || req.getIsPaid())
                .requiresApproval(req.getRequiresApproval() == null || req.getRequiresApproval())
                .carryoverAllowed(Boolean.TRUE.equals(req.getCarryoverAllowed()))
                .carryoverMaxDays(req.getCarryoverMaxDays() == null ? 0 : req.getCarryoverMaxDays())
                .description(req.getDescription())
                .build();
        LeaveType saved = typeRepo.save(type);
        LeaveTypeDtos.Response resp = mapper.toType(saved);
        events.audit("CREATE", "LEAVE_TYPE", saved.getId(), null, resp);
        return resp;
    }

    @Transactional
    public LeaveTypeDtos.Response update(UUID id, LeaveTypeDtos.UpsertRequest req) {
        LeaveType type = typeRepo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveType", id));
        LeaveTypeDtos.Response before = mapper.toType(type);

        type.setName(req.getName());
        if (req.getCode() != null && !req.getCode().isBlank()) {
            String newCode = req.getCode().toUpperCase();
            if (!newCode.equals(type.getCode())
                    && typeRepo.existsByCodeAndDeletedFalse(newCode)) {
                throw new BusinessException("Leave type with code already exists: " + newCode);
            }
            type.setCode(newCode);
        }
        type.setDaysAllowed(req.getDaysAllowed());
        if (req.getIsPaid() != null) type.setPaid(req.getIsPaid());
        if (req.getRequiresApproval() != null) type.setRequiresApproval(req.getRequiresApproval());
        if (req.getCarryoverAllowed() != null) type.setCarryoverAllowed(req.getCarryoverAllowed());
        if (req.getCarryoverMaxDays() != null) type.setCarryoverMaxDays(req.getCarryoverMaxDays());
        if (req.getDescription() != null) type.setDescription(req.getDescription());

        LeaveTypeDtos.Response after = mapper.toType(typeRepo.save(type));
        events.audit("UPDATE", "LEAVE_TYPE", id, before, after);
        return after;
    }

    @Transactional
    public void delete(UUID id) {
        LeaveType type = typeRepo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveType", id));
        LeaveTypeDtos.Response before = mapper.toType(type);
        type.setDeleted(true);
        typeRepo.save(type);
        events.audit("DELETE", "LEAVE_TYPE", id, before, null);
    }

    private static String resolveCode(String code, String name) {
        String src = (code == null || code.isBlank()) ? name : code;
        return src.trim().toUpperCase().replaceAll("[^A-Z0-9_]+", "_");
    }
}
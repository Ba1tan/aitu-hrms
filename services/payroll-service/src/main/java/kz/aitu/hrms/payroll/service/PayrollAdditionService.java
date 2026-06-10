package kz.aitu.hrms.payroll.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.payroll.dto.AdditionDtos;
import kz.aitu.hrms.payroll.entity.PayrollAddition;
import kz.aitu.hrms.payroll.entity.PayrollPeriod;
import kz.aitu.hrms.payroll.entity.PayrollPeriodStatus;
import kz.aitu.hrms.payroll.repository.PayrollAdditionRepository;
import kz.aitu.hrms.payroll.repository.PayrollPeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollAdditionService {

    private final PayrollAdditionRepository additionRepo;
    private final PayrollPeriodRepository periodRepo;
    private final PayrollMapper mapper;
    private final EventPublisher events;

    @Transactional(readOnly = true)
    public List<AdditionDtos.Response> list(UUID periodId, UUID employeeId) {
        if (periodId != null && employeeId != null) {
            return additionRepo.findByPeriodIdAndEmployeeIdAndDeletedFalse(periodId, employeeId)
                    .stream().map(mapper::toAdditionResponse).toList();
        }
        if (periodId != null) {
            return additionRepo.findByPeriodIdAndDeletedFalse(periodId)
                    .stream().map(mapper::toAdditionResponse).toList();
        }
        if (employeeId != null) {
            return additionRepo.findByEmployeeIdAndDeletedFalse(employeeId)
                    .stream().map(mapper::toAdditionResponse).toList();
        }
        throw new BusinessException("Provide at least one of periodId or employeeId");
    }

    @Transactional
    public AdditionDtos.Response create(AdditionDtos.CreateRequest req) {
        requireOpenPeriod(req.getPeriodId());
        PayrollAddition a = PayrollAddition.builder()
                .employeeId(req.getEmployeeId())
                .periodId(req.getPeriodId())
                .type(req.getType())
                .category(req.getCategory())
                .description(req.getDescription())
                .amount(req.getAmount())
                .taxable(req.getIsTaxable() == null || req.getIsTaxable())
                .build();
        PayrollAddition saved = additionRepo.save(a);
        AdditionDtos.Response resp = mapper.toAdditionResponse(saved);
        events.audit("CREATE", "PAYROLL_ADDITION", saved.getId(), null, resp);
        return resp;
    }

    @Transactional
    public AdditionDtos.Response update(UUID id, AdditionDtos.UpdateRequest req) {
        PayrollAddition a = require(id);
        requireOpenPeriod(a.getPeriodId());
        AdditionDtos.Response before = mapper.toAdditionResponse(a);
        if (req.getType() != null) a.setType(req.getType());
        if (req.getCategory() != null) a.setCategory(req.getCategory());
        if (req.getDescription() != null) a.setDescription(req.getDescription());
        if (req.getAmount() != null) a.setAmount(req.getAmount());
        if (req.getIsTaxable() != null) a.setTaxable(req.getIsTaxable());
        AdditionDtos.Response after = mapper.toAdditionResponse(additionRepo.save(a));
        events.audit("UPDATE", "PAYROLL_ADDITION", id, before, after);
        return after;
    }

    @Transactional
    public void delete(UUID id) {
        PayrollAddition a = require(id);
        requireOpenPeriod(a.getPeriodId());
        AdditionDtos.Response before = mapper.toAdditionResponse(a);
        a.setDeleted(true);
        additionRepo.save(a);
        events.audit("DELETE", "PAYROLL_ADDITION", id, before, null);
    }

    @Transactional
    public AdditionDtos.BulkResponse bulk(AdditionDtos.BulkRequest req) {
        requireOpenPeriod(req.getPeriodId());
        if (req.getEmployeeIds() == null || req.getEmployeeIds().isEmpty()) {
            throw new BusinessException("At least one employee is required for bulk addition");
        }
        List<UUID> ids = new ArrayList<>(req.getEmployeeIds().size());
        for (UUID empId : req.getEmployeeIds()) {
            PayrollAddition a = PayrollAddition.builder()
                    .employeeId(empId)
                    .periodId(req.getPeriodId())
                    .type(req.getType())
                    .category(req.getCategory())
                    .description(req.getDescription())
                    .amount(req.getAmount())
                    .taxable(req.getIsTaxable() == null || req.getIsTaxable())
                    .build();
            ids.add(additionRepo.save(a).getId());
        }
        events.audit("CREATE", "PAYROLL_ADDITION", null, null,
                Map.of("periodId", req.getPeriodId(), "type", String.valueOf(req.getType()),
                        "category", String.valueOf(req.getCategory()),
                        "amount", String.valueOf(req.getAmount()), "count", ids.size()));
        return AdditionDtos.BulkResponse.builder().created(ids.size()).ids(ids).build();
    }

    private PayrollAddition require(UUID id) {
        return additionRepo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollAddition", id));
    }

    private void requireOpenPeriod(UUID periodId) {
        PayrollPeriod p = periodRepo.findByIdAndDeletedFalse(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollPeriod", periodId));
        if (p.getStatus() == PayrollPeriodStatus.LOCKED
                || p.getStatus() == PayrollPeriodStatus.PAID
                || p.getStatus() == PayrollPeriodStatus.APPROVED) {
            throw new BusinessException(
                    "Cannot modify additions for period in status " + p.getStatus());
        }
    }
}
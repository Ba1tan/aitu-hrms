package kz.aitu.hrms.attendance.service;

import kz.aitu.hrms.attendance.dto.ScheduleDtos;
import kz.aitu.hrms.attendance.entity.WorkSchedule;
import kz.aitu.hrms.attendance.repository.WorkScheduleRepository;
import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkScheduleServiceImpl implements WorkScheduleService {

    private final WorkScheduleRepository scheduleRepo;
    private final AttendanceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleDtos.ScheduleResponse> list() {
        return scheduleRepo.findAllByDeletedFalseOrderByIsDefaultDescNameAsc().stream()
                .map(mapper::toSchedule)
                .toList();
    }

    @Override
    @Transactional
    public ScheduleDtos.ScheduleResponse create(ScheduleDtos.CreateScheduleRequest req) {
        validate(req.getWorkStartTime(), req.getWorkEndTime());

        boolean wantDefault = Boolean.TRUE.equals(req.getIsDefault());
        if (wantDefault) {
            clearExistingDefault();
        }
        if (req.getDepartmentId() != null
                && scheduleRepo.findByDepartmentIdAndDeletedFalse(req.getDepartmentId()).isPresent()) {
            throw new BusinessException("Schedule already exists for department " + req.getDepartmentId());
        }

        WorkSchedule s = WorkSchedule.builder()
                .name(req.getName())
                .workStartTime(req.getWorkStartTime())
                .workEndTime(req.getWorkEndTime())
                .lateThresholdMin(req.getLateThresholdMin() == null ? 15 : req.getLateThresholdMin())
                .halfDayThresholdMin(req.getHalfDayThresholdMin() == null ? 240 : req.getHalfDayThresholdMin())
                .departmentId(req.getDepartmentId())
                .isDefault(wantDefault)
                .build();
        return mapper.toSchedule(scheduleRepo.save(s));
    }

    @Override
    @Transactional
    public ScheduleDtos.ScheduleResponse update(UUID id, ScheduleDtos.UpdateScheduleRequest req) {
        WorkSchedule s = scheduleRepo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + id));

        if (req.getName() != null) s.setName(req.getName());
        if (req.getWorkStartTime() != null) s.setWorkStartTime(req.getWorkStartTime());
        if (req.getWorkEndTime() != null) s.setWorkEndTime(req.getWorkEndTime());
        if (req.getLateThresholdMin() != null) s.setLateThresholdMin(req.getLateThresholdMin());
        if (req.getHalfDayThresholdMin() != null) s.setHalfDayThresholdMin(req.getHalfDayThresholdMin());
        if (req.getDepartmentId() != null) s.setDepartmentId(req.getDepartmentId());
        if (Boolean.TRUE.equals(req.getIsDefault()) && !s.isDefault()) {
            clearExistingDefault();
            s.setDefault(true);
        } else if (Boolean.FALSE.equals(req.getIsDefault())) {
            s.setDefault(false);
        }

        validate(s.getWorkStartTime(), s.getWorkEndTime());
        return mapper.toSchedule(scheduleRepo.save(s));
    }

    private void validate(java.time.LocalTime start, java.time.LocalTime end) {
        if (start != null && end != null && !end.isAfter(start)) {
            throw new BusinessException("workEndTime must be after workStartTime");
        }
    }

    private void clearExistingDefault() {
        Optional<WorkSchedule> existing = scheduleRepo.findByIsDefaultTrueAndDeletedFalse();
        existing.ifPresent(s -> {
            s.setDefault(false);
            scheduleRepo.save(s);
        });
    }
}
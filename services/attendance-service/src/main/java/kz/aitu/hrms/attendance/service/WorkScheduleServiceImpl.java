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
    private final EventPublisher events;

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
                .workingDays(joinWorkingDays(req.getWorkingDays()))
                .description(req.getDescription())
                .isDefault(wantDefault)
                .build();
        WorkSchedule saved = scheduleRepo.save(s);
        ScheduleDtos.ScheduleResponse resp = mapper.toSchedule(saved);
        events.audit("CREATE", "WORK_SCHEDULE", saved.getId(), null, resp);
        return resp;
    }

    @Override
    @Transactional
    public ScheduleDtos.ScheduleResponse update(UUID id, ScheduleDtos.UpdateScheduleRequest req) {
        WorkSchedule s = scheduleRepo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + id));
        ScheduleDtos.ScheduleResponse before = mapper.toSchedule(s);

        if (req.getName() != null) s.setName(req.getName());
        if (req.getWorkStartTime() != null) s.setWorkStartTime(req.getWorkStartTime());
        if (req.getWorkEndTime() != null) s.setWorkEndTime(req.getWorkEndTime());
        if (req.getLateThresholdMin() != null) s.setLateThresholdMin(req.getLateThresholdMin());
        if (req.getHalfDayThresholdMin() != null) s.setHalfDayThresholdMin(req.getHalfDayThresholdMin());
        if (req.getDepartmentId() != null) s.setDepartmentId(req.getDepartmentId());
        if (req.getWorkingDays() != null) s.setWorkingDays(joinWorkingDays(req.getWorkingDays()));
        if (req.getDescription() != null) s.setDescription(req.getDescription());
        if (Boolean.TRUE.equals(req.getIsDefault()) && !s.isDefault()) {
            clearExistingDefault();
            s.setDefault(true);
        } else if (Boolean.FALSE.equals(req.getIsDefault())) {
            s.setDefault(false);
        }

        validate(s.getWorkStartTime(), s.getWorkEndTime());
        ScheduleDtos.ScheduleResponse after = mapper.toSchedule(scheduleRepo.save(s));
        events.audit("UPDATE", "WORK_SCHEDULE", id, before, after);
        return after;
    }

    private void validate(java.time.LocalTime start, java.time.LocalTime end) {
        if (start != null && end != null && !end.isAfter(start)) {
            throw new BusinessException("workEndTime must be after workStartTime");
        }
    }

    /**
     * Normalises a list of day codes into the persisted CSV form. Empty /
     * null falls back to Mon–Fri so the column never holds a blank row.
     */
    private String joinWorkingDays(java.util.List<String> days) {
        if (days == null || days.isEmpty()) return "MON,TUE,WED,THU,FRI";
        return String.join(",", days.stream()
                .map(d -> d == null ? "" : d.trim().toUpperCase())
                .filter(d -> !d.isBlank())
                .toList());
    }

    private void clearExistingDefault() {
        Optional<WorkSchedule> existing = scheduleRepo.findByIsDefaultTrueAndDeletedFalse();
        existing.ifPresent(s -> {
            s.setDefault(false);
            // Flush immediately: otherwise Hibernate orders the new default's
            // INSERT before this UPDATE on commit, and the two rows transiently
            // collide on the idx_work_schedules_default partial unique index.
            scheduleRepo.saveAndFlush(s);
        });
    }
}
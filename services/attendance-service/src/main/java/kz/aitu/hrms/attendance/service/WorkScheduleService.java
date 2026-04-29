package kz.aitu.hrms.attendance.service;

import kz.aitu.hrms.attendance.dto.ScheduleDtos;

import java.util.List;
import java.util.UUID;

public interface WorkScheduleService {

    List<ScheduleDtos.ScheduleResponse> list();

    ScheduleDtos.ScheduleResponse create(ScheduleDtos.CreateScheduleRequest req);

    ScheduleDtos.ScheduleResponse update(UUID id, ScheduleDtos.UpdateScheduleRequest req);
}
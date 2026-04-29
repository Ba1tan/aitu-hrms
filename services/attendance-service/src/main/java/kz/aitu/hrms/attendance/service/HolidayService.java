package kz.aitu.hrms.attendance.service;

import kz.aitu.hrms.attendance.dto.HolidayDtos;

import java.util.List;
import java.util.UUID;

public interface HolidayService {

    List<HolidayDtos.HolidayResponse> list(Integer year);

    HolidayDtos.HolidayResponse create(HolidayDtos.CreateHolidayRequest req);

    HolidayDtos.HolidayResponse update(UUID id, HolidayDtos.UpdateHolidayRequest req);

    void delete(UUID id);
}
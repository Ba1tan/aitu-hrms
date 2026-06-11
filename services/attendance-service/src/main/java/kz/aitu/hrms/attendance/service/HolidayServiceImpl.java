package kz.aitu.hrms.attendance.service;

import kz.aitu.hrms.attendance.dto.HolidayDtos;
import kz.aitu.hrms.attendance.entity.Holiday;
import kz.aitu.hrms.attendance.repository.HolidayRepository;
import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HolidayServiceImpl implements HolidayService {

    private final HolidayRepository holidayRepo;
    private final AttendanceMapper mapper;
    private final EventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public List<HolidayDtos.HolidayResponse> list(Integer year) {
        // Filter by year via an explicit date range — portable across databases.
        // (A SQL YEAR() function exists in H2 but NOT in PostgreSQL, where it
        //  would throw and surface to the UI as an empty calendar.)
        List<Holiday> holidays = (year == null)
                ? holidayRepo.findAllActive()
                : holidayRepo.findBetween(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        return holidays.stream()
                .map(mapper::toHoliday)
                .toList();
    }

    @Override
    @Transactional
    public HolidayDtos.HolidayResponse create(HolidayDtos.CreateHolidayRequest req) {
        if (holidayRepo.existsByHolidayDateAndDeletedFalse(req.getHolidayDate())) {
            throw new BusinessException("Holiday already exists on " + req.getHolidayDate());
        }
        Holiday h = Holiday.builder()
                .name(req.getName())
                .holidayDate(req.getHolidayDate())
                .annual(Boolean.TRUE.equals(req.getAnnual()))
                .description(req.getDescription())
                .build();
        Holiday saved = holidayRepo.save(h);
        HolidayDtos.HolidayResponse resp = mapper.toHoliday(saved);
        events.audit("CREATE", "HOLIDAY", saved.getId(), null, resp);
        return resp;
    }

    @Override
    @Transactional
    public HolidayDtos.HolidayResponse update(UUID id, HolidayDtos.UpdateHolidayRequest req) {
        Holiday h = holidayRepo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found: " + id));
        HolidayDtos.HolidayResponse before = mapper.toHoliday(h);
        if (req.getName() != null) h.setName(req.getName());
        if (req.getHolidayDate() != null) {
            if (!req.getHolidayDate().equals(h.getHolidayDate())
                    && holidayRepo.existsByHolidayDateAndDeletedFalse(req.getHolidayDate())) {
                throw new BusinessException("Another holiday already exists on " + req.getHolidayDate());
            }
            h.setHolidayDate(req.getHolidayDate());
        }
        if (req.getAnnual() != null) h.setAnnual(req.getAnnual());
        if (req.getDescription() != null) h.setDescription(req.getDescription());
        HolidayDtos.HolidayResponse after = mapper.toHoliday(holidayRepo.save(h));
        events.audit("UPDATE", "HOLIDAY", id, before, after);
        return after;
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Holiday h = holidayRepo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found: " + id));
        HolidayDtos.HolidayResponse before = mapper.toHoliday(h);
        h.setDeleted(true);
        holidayRepo.save(h);
        events.audit("DELETE", "HOLIDAY", id, before, null);
    }
}
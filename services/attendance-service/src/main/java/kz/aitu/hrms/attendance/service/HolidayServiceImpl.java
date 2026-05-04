package kz.aitu.hrms.attendance.service;

import kz.aitu.hrms.attendance.dto.HolidayDtos;
import kz.aitu.hrms.attendance.entity.Holiday;
import kz.aitu.hrms.attendance.repository.HolidayRepository;
import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HolidayServiceImpl implements HolidayService {

    private final HolidayRepository holidayRepo;
    private final AttendanceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<HolidayDtos.HolidayResponse> list(Integer year) {
        return holidayRepo.findByYear(year).stream()
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
        return mapper.toHoliday(holidayRepo.save(h));
    }

    @Override
    @Transactional
    public HolidayDtos.HolidayResponse update(UUID id, HolidayDtos.UpdateHolidayRequest req) {
        Holiday h = holidayRepo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found: " + id));
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
        return mapper.toHoliday(holidayRepo.save(h));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Holiday h = holidayRepo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found: " + id));
        h.setDeleted(true);
        holidayRepo.save(h);
    }
}
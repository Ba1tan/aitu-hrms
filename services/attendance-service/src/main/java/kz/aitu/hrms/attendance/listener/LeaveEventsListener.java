package kz.aitu.hrms.attendance.listener;

import kz.aitu.hrms.attendance.config.RabbitConfig;
import kz.aitu.hrms.attendance.entity.AttendanceRecord;
import kz.aitu.hrms.attendance.entity.AttendanceStatus;
import kz.aitu.hrms.attendance.repository.AttendanceRecordRepository;
import kz.aitu.hrms.common.event.LeaveApprovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Listens for {@code leave.approved} events from leave-service and seeds
 * ON_LEAVE rows into attendance_records for every weekday in the approved
 * range. If a record already exists for that (employee, date) we update
 * the status only when it is still ABSENT — anything else (PRESENT, LATE,
 * HALF_DAY) means the employee actually showed up so we leave it alone.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeaveEventsListener {

    private final AttendanceRecordRepository recordRepo;

    @RabbitListener(queues = RabbitConfig.QUEUE_LEAVE_APPROVED)
    @Transactional
    public void onLeaveApproved(LeaveApprovedEvent event) {
        if (event == null || event.getEmployeeId() == null
                || event.getStartDate() == null || event.getEndDate() == null) {
            log.warn("Ignoring malformed LeaveApprovedEvent: {}", event);
            return;
        }
        log.info("Marking ON_LEAVE for employee {} {} -> {}",
                event.getEmployeeId(), event.getStartDate(), event.getEndDate());

        List<AttendanceRecord> toSave = new ArrayList<>();
        LocalDate cursor = event.getStartDate();
        while (!cursor.isAfter(event.getEndDate())) {
            LocalDate day = cursor;
            cursor = cursor.plusDays(1);

            // Skip weekends — they don't get attendance rows in the first place.
            if (day.getDayOfWeek().getValue() >= 6) continue;

            recordRepo.findByEmployeeIdAndWorkDateAndDeletedFalse(event.getEmployeeId(), day)
                    .ifPresentOrElse(
                            existing -> {
                                if (existing.getStatus() == AttendanceStatus.ABSENT) {
                                    existing.setStatus(AttendanceStatus.ON_LEAVE);
                                    existing.setNotes(appendNote(existing.getNotes(),
                                            "ON_LEAVE via " + event.getLeaveType()));
                                    recordRepo.save(existing);
                                }
                            },
                            () -> toSave.add(AttendanceRecord.builder()
                                    .employeeId(event.getEmployeeId())
                                    .workDate(day)
                                    .status(AttendanceStatus.ON_LEAVE)
                                    .overtimeMinutes(0)
                                    .notes("ON_LEAVE via " + event.getLeaveType())
                                    .build())
                    );
        }
        if (!toSave.isEmpty()) {
            recordRepo.saveAll(toSave);
        }
    }

    private String appendNote(String existing, String addition) {
        if (existing == null || existing.isBlank()) return addition;
        return existing + "; " + addition;
    }
}
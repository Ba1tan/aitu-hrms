package kz.aitu.hrms.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "work_schedules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkSchedule extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "work_start_time", nullable = false)
    private LocalTime workStartTime;

    @Column(name = "work_end_time", nullable = false)
    private LocalTime workEndTime;

    @Column(name = "late_threshold_min", nullable = false)
    @Builder.Default
    private Integer lateThresholdMin = 15;

    @Column(name = "half_day_threshold_min", nullable = false)
    @Builder.Default
    private Integer halfDayThresholdMin = 240;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    /**
     * In-memory fallback used when no schedule row exists in the DB.
     * Matches the seed default in V1__init_attendance_schema.sql.
     */
    public static WorkSchedule fallback() {
        return WorkSchedule.builder()
                .name("Standard 9-18 (fallback)")
                .workStartTime(LocalTime.of(9, 0))
                .workEndTime(LocalTime.of(18, 0))
                .lateThresholdMin(15)
                .halfDayThresholdMin(240)
                .isDefault(true)
                .build();
    }
}
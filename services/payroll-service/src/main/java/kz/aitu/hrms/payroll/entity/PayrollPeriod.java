package kz.aitu.hrms.payroll.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payroll_periods")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPeriod extends BaseEntity {

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "working_days", nullable = false)
    private Integer workingDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PayrollPeriodStatus status = PayrollPeriodStatus.DRAFT;

    @Column(name = "processed_by")
    private UUID processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "batch_job_id")
    private Long batchJobId;

    /** Human-readable period name, e.g. "Март 2026". */
    public String getName() {
        String[] monthsRu = {
            "", "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        };
        return monthsRu[month] + " " + year;
    }
}
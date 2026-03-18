package kz.aitu.hrms.modules.payroll.entity;

import jakarta.persistence.*;
import kz.aitu.hrms.common.audit.BaseEntity;
import kz.aitu.hrms.modules.payroll.enums.PayrollPeriodStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payroll_periods",
        uniqueConstraints = @UniqueConstraint(columnNames = {"year", "month"}))
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
    @Column(nullable = false)
    @Builder.Default
    private PayrollPeriodStatus status = PayrollPeriodStatus.DRAFT;

    // Who triggered payroll processing
    @Column(name = "processed_by")
    private UUID processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    // Who approved payout
    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * readable period name, "Март 2024"
     */
    public String getName() {
        String[] monthsRu = {
            "", "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        };
        return monthsRu[month] + " " + year;
    }
}

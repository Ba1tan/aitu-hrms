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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "salary_advances")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryAdvance extends BaseEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate;

    @Column(name = "repayment_start", nullable = false)
    private LocalDate repaymentStart;

    @Column(nullable = false)
    private Integer installments;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal remaining;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AdvanceStatus status = AdvanceStatus.ACTIVE;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(columnDefinition = "text")
    private String notes;
}
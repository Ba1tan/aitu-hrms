package kz.aitu.hrms.payroll.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A payslip is a cross-service snapshot. Identity-side fields (employee_id,
 * iin, name, department, position) are duplicated locally so payslips remain
 * readable when employee-service is unavailable, when an employee is later
 * renamed, or after a period is locked years later.
 */
@Entity
@Table(name = "payslips")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payslip extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private PayrollPeriod period;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "employee_iin", length = 12)
    private String employeeIin;

    @Column(name = "employee_name", length = 300)
    private String employeeName;

    @Column(name = "employee_number", length = 30)
    private String employeeNumber;

    @Column(name = "department_name", length = 200)
    private String departmentName;

    @Column(name = "position_title", length = 200)
    private String positionTitle;

    // ── Days ───────────────────────────────────────────────────────────────
    @Column(name = "worked_days", nullable = false)
    private Integer workedDays;

    @Column(name = "total_working_days", nullable = false)
    private Integer totalWorkingDays;

    // ── Earnings ───────────────────────────────────────────────────────────
    @Column(name = "gross_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossSalary;

    @Column(name = "earned_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal earnedSalary;

    @Column(name = "allowances", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal allowances = BigDecimal.ZERO;

    @Column(name = "other_deductions", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal otherDeductions = BigDecimal.ZERO;

    // ── Employee deductions ────────────────────────────────────────────────
    @Column(name = "opv_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal opvAmount;

    @Column(name = "vosms_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal vosmsAmount = BigDecimal.ZERO;

    @Column(name = "oopv_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal oopvAmount = BigDecimal.ZERO;

    @Column(name = "taxable_income", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxableIncome;

    @Column(name = "ipn_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal ipnAmount;

    @Column(name = "total_deductions", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDeductions;

    @Column(name = "net_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal netSalary;

    // ── Employer obligations (informational, NOT deducted) ─────────────────
    @Column(name = "so_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal soAmount;

    @Column(name = "sn_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal snAmount;

    @Column(name = "opvr_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal opvrAmount = BigDecimal.ZERO;

    // ── Calculation metadata ───────────────────────────────────────────────
    @Column(name = "mrp_used", nullable = false)
    private Integer mrpUsed;

    @Column(name = "is_resident", nullable = false)
    @Builder.Default
    private boolean resident = true;

    @Column(name = "has_disability", nullable = false)
    @Builder.Default
    private boolean disability = false;

    // ── Status & AI ────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PayslipStatus status = PayslipStatus.DRAFT;

    @Column(name = "anomaly_score", precision = 5, scale = 4)
    private BigDecimal anomalyScore;

    @Column(name = "anomaly_flags", columnDefinition = "text")
    private String anomalyFlags;

    @Column(name = "ai_reviewed", nullable = false)
    @Builder.Default
    private boolean aiReviewed = false;

    @Column(name = "ai_reviewed_by")
    private UUID aiReviewedBy;

    @Column(name = "ai_reviewed_at")
    private LocalDateTime aiReviewedAt;

    @Column(name = "pdf_url", columnDefinition = "text")
    private String pdfUrl;
}
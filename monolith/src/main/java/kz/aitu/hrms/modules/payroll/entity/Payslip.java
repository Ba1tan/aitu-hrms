package kz.aitu.hrms.modules.payroll.entity;

import jakarta.persistence.*;
import kz.aitu.hrms.common.audit.BaseEntity;
import kz.aitu.hrms.modules.employee.entity.Employee;
import kz.aitu.hrms.modules.payroll.enums.PayslipStatus;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "payslips",
        uniqueConstraints = @UniqueConstraint(columnNames = {"period_id", "employee_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payslip extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private PayrollPeriod period;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // ---- Days ----
    @Column(name = "worked_days", nullable = false)
    private Integer workedDays;

    @Column(name = "total_working_days", nullable = false)
    private Integer totalWorkingDays;

    // ---- Earnings ----
    @Column(name = "gross_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossSalary;

    @Column(name = "earned_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal earnedSalary;        // Prorated if partial month

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal allowances = BigDecimal.ZERO;

    // ---- Employee deductions ----
    @Column(name = "opv_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal opvAmount;           // ОПВ pension 10%

    @Column(name = "oopv_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal oopvAmount = BigDecimal.ZERO;  // ООПВ 1.5% (if applicable)

    @Column(name = "taxable_income", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxableIncome;

    @Column(name = "ipn_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal ipnAmount;           // ИПН income tax 10%

    @Column(name = "other_deductions", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal otherDeductions = BigDecimal.ZERO;

    @Column(name = "total_deductions", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDeductions;

    @Column(name = "net_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal netSalary;

    // ---- Employer obligations (not deducted from employee) ----
    @Column(name = "so_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal soAmount;            // СО 3.5%

    @Column(name = "sn_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal snAmount;            // СН 9.5% - SO

    // ---- Metadata ----
    @Column(name = "mrp_used", nullable = false)
    private Integer mrpUsed;

    @Column(name = "is_resident", nullable = false)
    private boolean resident;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PayslipStatus status = PayslipStatus.DRAFT;

    @Column(name = "pdf_url")
    private String pdfUrl;
}

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
import java.util.UUID;

@Entity
@Table(name = "payroll_additions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollAddition extends BaseEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "period_id", nullable = false)
    private UUID periodId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdditionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AdditionCategory category;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "is_taxable", nullable = false)
    @Builder.Default
    private boolean taxable = true;
}
package kz.aitu.hrms.modules.employee.entity;

import jakarta.persistence.*;
import kz.aitu.hrms.common.audit.BaseEntity;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "positions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position extends BaseEntity {

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "min_salary", precision = 15, scale = 2)
    private BigDecimal minSalary;

    @Column(name = "max_salary", precision = 15, scale = 2)
    private BigDecimal maxSalary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;
}

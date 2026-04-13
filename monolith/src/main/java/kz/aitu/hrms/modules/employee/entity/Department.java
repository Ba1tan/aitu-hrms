package kz.aitu.hrms.modules.employee.entity;

import jakarta.persistence.*;
import kz.aitu.hrms.common.audit.BaseEntity;
import lombok.*;

@Entity
@Table(name = "departments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Department extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(name = "cost_center")
    private String costCenter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Department parent;
}

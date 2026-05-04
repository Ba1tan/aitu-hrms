package kz.aitu.hrms.leave.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "leave_types")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveType extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(name = "days_allowed", nullable = false)
    private int daysAllowed;

    @Column(name = "is_paid", nullable = false)
    @Builder.Default
    private boolean isPaid = true;

    @Column(name = "requires_approval", nullable = false)
    @Builder.Default
    private boolean requiresApproval = true;

    @Column(name = "carryover_allowed", nullable = false)
    @Builder.Default
    private boolean carryoverAllowed = false;

    @Column(name = "carryover_max_days", nullable = false)
    @Builder.Default
    private int carryoverMaxDays = 0;

    @Column(columnDefinition = "TEXT")
    private String description;
}
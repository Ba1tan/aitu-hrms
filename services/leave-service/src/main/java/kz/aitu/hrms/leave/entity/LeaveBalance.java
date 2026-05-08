package kz.aitu.hrms.leave.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * {@code remainingDays} is a database-generated column
 * ({@code entitledDays + carriedOver + adjustedDays - usedDays}) — it is
 * read-only at the JPA layer. Mutate the inputs and reload to see the new value.
 */
@Entity
@Table(name = "leave_balances")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalance extends BaseEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(nullable = false)
    private int year;

    @Column(name = "entitled_days", nullable = false)
    @Builder.Default
    private int entitledDays = 0;

    @Column(name = "carried_over", nullable = false)
    @Builder.Default
    private int carriedOver = 0;

    @Column(name = "used_days", nullable = false)
    @Builder.Default
    private int usedDays = 0;

    @Column(name = "adjusted_days", nullable = false)
    @Builder.Default
    private int adjustedDays = 0;

    @Column(name = "remaining_days", insertable = false, updatable = false)
    private int remainingDays;
}
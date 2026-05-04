package kz.aitu.hrms.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "holidays")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Holiday extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(name = "is_annual", nullable = false)
    @Builder.Default
    private boolean annual = true;

    @Column(columnDefinition = "TEXT")
    private String description;
}
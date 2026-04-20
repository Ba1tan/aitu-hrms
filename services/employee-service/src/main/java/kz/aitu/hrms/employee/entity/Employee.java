package kz.aitu.hrms.employee.entity;

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
import java.time.LocalDate;

@Entity
@Table(name = "employees")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee extends BaseEntity {

    @Column(name = "employee_number", nullable = false, unique = true, length = 30)
    private String employeeNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(length = 12, unique = true)
    private String iin;

    @Column(length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "termination_reason", columnDefinition = "text")
    private String terminationReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmploymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", length = 20)
    private EmploymentType employmentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @Column(name = "base_salary", nullable = false, precision = 14, scale = 2)
    private BigDecimal baseSalary;

    @Enumerated(EnumType.STRING)
    @Column(name = "disability_group", nullable = false, length = 10)
    private DisabilityGroup disabilityGroup;

    @Column(name = "is_resident", nullable = false)
    private boolean resident = true;

    @Column(name = "is_pensioner", nullable = false)
    private boolean pensioner = false;

    @Column(columnDefinition = "text")
    private String address;

    public String getFullName() {
        if (middleName != null && !middleName.isBlank()) {
            return lastName + " " + firstName + " " + middleName;
        }
        return lastName + " " + firstName;
    }
}
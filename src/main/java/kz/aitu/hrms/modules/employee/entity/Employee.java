package kz.aitu.hrms.modules.employee.entity;

import jakarta.persistence.*;
import kz.aitu.hrms.common.audit.BaseEntity;
import lombok.*;

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

    @Column(name = "employee_number", nullable = false, unique = true)
    private String employeeNumber;   // e.g. EMP-0001

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "middle_name")
    private String middleName;       // Отчество

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "iin", unique = true)
    private String iin;              // ИИН (Individual Identification Number)

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmploymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false)
    private EmploymentType employmentType;

    @Column(name = "base_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal baseSalary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @Column(name = "bank_account")
    private String bankAccount;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    // Kazakhstan-specific
    @Column(name = "is_resident", nullable = false)
    private boolean resident = true;

    @Column(name = "has_disability")
    private boolean hasDisability = false;

    @Column(name = "is_pensioner")
    private boolean isPensioner = false;

    public String getFullName() {
        if (middleName != null && !middleName.isBlank()) {
            return lastName + " " + firstName + " " + middleName;
        }
        return lastName + " " + firstName;
    }
}
